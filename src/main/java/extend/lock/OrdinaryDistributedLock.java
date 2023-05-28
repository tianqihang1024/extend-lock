package extend.lock;


import extend.listener.PublishSubscribe;
import extend.listener.SyncQueue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * @author 田奇杭
 * @Description 普通分布式锁
 * @Date 2023/5/10 21:11
 */
@Slf4j
@Service
public class OrdinaryDistributedLock extends AbstractDistributedLock {

    /**
     * 普通分布式锁lua脚本-抢占
     * KEYS[1]: 锁名称
     * ARGV[1]: 锁持续时间
     * ARGV[2]: 线程标识
     */
    private static final String ORDINARY_LOCK_SCRIPT = "if (redis.call('EXISTS', KEYS[1]) == 0) then \n" +
            "    redis.call('HINCRBY', KEYS[1], ARGV[2], 1); \n" +
            "    redis.call('PEXPIRE', KEYS[1], ARGV[1]); \n" +
            "    return nil; \n" +
            "    end; \n" +
            "if (redis.call('HEXISTS', KEYS[1], ARGV[2]) == 1) then\n" +
            "    redis.call('HINCRBY', KEYS[1], ARGV[2], 1); \n" +
            "    redis.call('PEXPIRE', KEYS[1], ARGV[1]); \n" +
            "    return nil; \n" +
            "    end; \n" +
            "return redis.call('PTTL', KEYS[1]);";

    /**
     * 普通分布式锁lua脚本-释放
     * KEYS[1]: 锁名称
     * ARGV[1]: 锁持续时间
     * ARGV[2]: 线程标识
     */
    private static final String ORDINARY_UNLOCK_SCRIPT = "if (redis.call('HGET', KEYS[1], ARGV[2]) > 1) then \n" +
            "    redis.call('HDECRBY', KEYS[1], ARGV[2], 1); \n" +
            "    redis.call('PEXPIRE', KEYS[1], ARGV[1]); \n" +
            "    return nil; \n" +
            "    end; \n" +
            "if (redis.call('HGET', KEYS[1], ARGV[2]) == 1) then \n" +
            "    redis.call('HDECRBY', KEYS[1], ARGV[2], 1); \n" +
            "    redis.call('DEL', KEYS[1]); \n" +
            "    redis.call('PUBLISH', 'UN_LOCK_TOPIC', KEYS[1]); \n" +
            "    return nil; \n" +
            "    end; \n" +
            "return redis.call('PTTL', KEYS[1]);";

    /**
     * redis 操作对象
     */
    @Resource
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 尝试获取锁
     *
     * @param key       锁名称
     * @param waitTime  等待时间
     * @param leaseTime 锁持续时间
     * @param unit      时间单位
     * @return true:抢占成功 false:抢占失败
     */
    @Override
    public boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit unit) {

        // 格式化参数
        leaseTime = unit.toNanos(leaseTime);
        long threadFlag = AbstractDistributedLock.THREAD_FLAG + Thread.currentThread().getId();

        // 组装锁名称
        String lockName = assembleLockName(key);

        // 获取分布式锁对应的JVM本地锁对象
        SyncQueue syncQueue = PublishSubscribe.getSyncQueueByLockName(lockName);

        // 获取JVM本地锁
        boolean flag = syncQueue.tryAcquire();

        // 获取JVM本地锁成功 || 尝试获取结果
        while (flag || syncQueue.acquire(waitTime)) {
            // 尝试设置分布式锁
            Long ttl = tryAcquireDistributedLock(key, leaseTime, threadFlag);
            // 设置分布式锁成功
            if (ttl == null)
                // 抢占成功返回 true
                return true;
            else
                // 分布式锁已被抢占，返回 false 进入 syncQueue.tryAcquire 方法中阻塞
                flag = false;
        }
        // 未抢占成功返回 false
        return false;
    }

    /**
     * 获取锁
     *
     * @param key 锁名称
     * @return true：抢占成功 false：抢占失败
     */
    @Override
    public boolean lock(String key) {
        return false;
    }

    /**
     * 释放锁
     *
     * @param key       锁名称
     * @param leaseTime 锁持续时间
     * @param unit      时间单位
     * @return true：释放锁成功 false：释放锁失败
     */
    @Override
    public boolean unLock(String key, long leaseTime, TimeUnit unit) {

        // 格式化参数
        leaseTime = unit.toNanos(leaseTime);
        long threadId = Thread.currentThread().getId();

        // 组装锁名称
        String lockName = assembleLockName(key);

        // 获取分布式锁对应的JVM本地锁对象
        SyncQueue syncQueue = PublishSubscribe.getSyncQueueByLockName(lockName);

        // 释放JVM锁
        syncQueue.release();

        // 执行释放分布式锁脚本
        Long flag = unDistributedLock(lockName, leaseTime, threadId);

        return flag == null;
    }

    /**
     * 组装锁名称
     *
     * @param keyword 关键字
     * @return 锁名称
     */
    @Override
    public String assembleLockName(String keyword) {
        return String.format(AbstractDistributedLock.LOCK_NAME_FORMAT, "ordinary", keyword);
    }

    /**
     * 尝试获取分布式锁
     *
     * @param key      锁名称
     * @param current  持续时间
     * @param threadId 线程标识
     * @return null:成功 !null:失败
     */
    private Long tryAcquireDistributedLock(String key, long current, long threadId) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(OrdinaryDistributedLock.ORDINARY_LOCK_SCRIPT, Long.class);
        try {
            return redisTemplate.execute(script, Collections.singletonList(key), current, threadId);
        } catch (Exception e) {
            log.error("tryAcquireDistributedLock fail key:{}, current:{}, threadId:{}", key, current, threadId);
        }
        return 0L;
    }

    /**
     * 释放分布式锁
     *
     * @param key      锁名称
     * @param current  持续时间
     * @param threadId 线程标识
     * @return null:成功 !null:失败
     */
    private Long unDistributedLock(String key, long current, long threadId) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(OrdinaryDistributedLock.ORDINARY_UNLOCK_SCRIPT, Long.class);
        try {
            return redisTemplate.execute(script, Collections.singletonList(key), current, threadId);
        } catch (Exception e) {
            log.error("unDistributedLock fail key:{}, current:{}, threadId:{}", key, current, threadId);
        }
        return 0L;
    }

}
