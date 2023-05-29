package extend.listener;

import com.alibaba.fastjson.JSON;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 田奇杭
 * @Description 分布式锁释放监听类
 * @Date 2023/5/14 22:03
 */
@Slf4j
@Component
public class PublishSubscribe implements MessageListener {

    /**
     * 等待订阅消息的线程集合
     * key: 锁名称
     * value: 等待锁释放的线程 set 集合
     */
    private static final Map<String, SyncQueue> SYNC_QUEUE_MAP = new ConcurrentHashMap<>();

    /**
     * 监听锁释放 Topic 根据释放的锁名称获取对应的同步节点
     * 在对列不为空的情况下，尝试唤醒头部节点，使其能够参加到分布式锁的抢占中
     *
     * @param message 释放的锁名称
     * @param pattern pattern matching the channel (if specified) - can be {@literal null}.
     */
    @Override
    public void onMessage(@NonNull Message message, byte[] pattern) {
        log.info("PublishSubscribe.onMessage message:{} pattern:{}", message, pattern);
        // 获取释放的临界资源名字
        String unLockName = message.toString();
        // 尝试获取被释放锁的本地同步队列
        SyncQueue syncQueue = SYNC_QUEUE_MAP.get(unLockName);
        log.info("PublishSubscribe.syncQueue syncQueue:{}", JSON.toJSONString(syncQueue));
        // 本地同步队列可能为空，因为可能压根就没有针对这个临界资源的操作
        if (syncQueue != null) {
            // 唤醒头部节点使其能够苏醒，参与到临界资源的抢占中
            syncQueue.doSignal();
        }
    }

    /**
     * 根据锁名称获取同步队列
     *
     * @param lockName 锁名称
     * @return 同步队列
     */
    public static SyncQueue getSyncQueueByLockName(String lockName) {
        return SYNC_QUEUE_MAP.computeIfAbsent(lockName, k -> new SyncQueue());
    }

    /**
     * 根据锁名称删除同步队列
     *
     * @param lockName 锁名称
     */
    public static SyncQueue deleteSyncQueueByLockName(String lockName) {
        return SYNC_QUEUE_MAP.remove(lockName);
    }


}
