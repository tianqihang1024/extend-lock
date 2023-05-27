package extend.lock;


import java.util.concurrent.TimeUnit;

/**
 * @author 田奇杭
 * @Description 分布式锁基类
 * @Date 2023/5/10 23:48
 */
public abstract class AbstractDistributedLock {

    /**
     * 锁名称基本格式
     * 锁类型:{关键字}
     */
    protected static final String LOCK_NAME_FORMAT = "%s:{%s}";

    /**
     * 线程标识，与线程ID拼接保证多服务器下的抢占线程唯一，
     * 避免多服务器下线程名称一致导致的错误重入（系统启动时的时间戳能够满足随机需求，后期可以考虑使用雪花算法，那样更安全）
     */
    protected static final Long THREAD_FLAG = System.currentTimeMillis();

    /**
     * 尝试获取锁
     *
     * @param key       锁名称
     * @param waitTime  等待时间
     * @param leaseTime 锁持续时间
     * @param unit      时间单位
     * @return true:抢占成功 false:抢占失败
     */
    abstract boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit unit);

    /**
     * 获取锁
     *
     * @param key 锁名称
     * @return true：抢占成功 false：抢占失败
     */
    abstract boolean lock(String key);

    /**
     * 释放锁
     *
     * @param key 锁名称
     * @param leaseTime 锁持续时间
     * @param unit      时间单位
     * @return true：释放锁成功 false：释放锁失败
     */
    abstract boolean ubLock(String key, long leaseTime, TimeUnit unit);

    /**
     * 组装锁名称
     *
     * @param keyword 关键字
     * @return 锁名称
     */
    abstract String assembleLockName(String keyword);


}
