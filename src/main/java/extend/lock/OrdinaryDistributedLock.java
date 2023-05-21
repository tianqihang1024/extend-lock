package extend.lock;


import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * @author 田奇杭
 * @Description
 * @Date 2023/5/10 21:11
 */
@Slf4j
@Service
public class OrdinaryDistributedLock extends AbstractDistributedLock {

    /**
     * 普通分布式锁lua脚本-抢占
     * KEYS[1]: 锁名称
     * KEYS[2]: FIFO 线程队列名称
     * ARGV[1]: 锁持续时间
     * ARGV[2]: 线程标识
     * ARGV[3]: 发起抢占的时间戳
     */
    private static final String ORDINARY_LOCK_SCRIPT = "if(redis.call('EXISTS', KEYS[1]) == 0) then" +
            "   redis.call('SET', KEYS[1], ARGV[1]);" +
            "   redis.call('PEXPIRE', KEYS[1], ARGV[2]);" +
            "   return nil;" +
            "   end;" +
            "return redis.call('PTTL', KEYS[1]);";

    /**
     * 普通分布式锁lua脚本-释放
     */
    private static final String ORDINARY_UNLOCK_SCRIPT = "";

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Override
    boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit unit) {

        // 格式化参数
        long time = unit.toMillis(waitTime);
        long current = System.currentTimeMillis();
        long threadId = Thread.currentThread().getId();

        // 尝试获取锁
        Long ttl = tryAcquire(key, current, threadId);
        if (ttl == null) {
            return true;
        } else {
            time -= System.currentTimeMillis() - current;
            // 申请锁的耗时如果大于等于最大等待时间，则申请锁失败
            if (time <= 0) {
                return false;
            } else {
                String channelName = getChannelName("");
            }


        }


        return false;
    }

    @Override
    boolean lock(String key) {
        return false;
    }

    @Override
    boolean ubLock(String key) {
        return false;
    }

    /**
     * 尝试获取锁
     *
     * @param key      锁名称
     * @param current  持续时间
     * @param threadId 线程标识
     * @return true:成功 false:失败
     */
    private Long tryAcquire(String key, long current, long threadId) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        try {
            return redisTemplate.execute(script, Collections.singletonList(key), current, threadId);
        } catch (Exception e) {
            log.error("tryAcquire fail key:{}, current:{}, threadId:{}", key, current, threadId);
        }
        return null;
    }


}
