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
     */
    protected String lockNameFormat = "%s:{%s}";

    /**
     * 获取监听通道名称
     *
     * @param name 锁名称
     * @return 监听通道名称
     */
    protected String getChannelName(String name) {
        String prefix = "redis_lock__channel";
        return name.contains("{") ? prefix + ":" + name : prefix + ":{" + name + "}";
    }

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
     * @return true：释放锁成功 false：释放锁失败
     */
    abstract boolean ubLock(String key);

    /**
     * 组装锁名称
     *
     * @param keyword 关键字
     * @return 锁名称
     */
    abstract String assembleLockName(String keyword);


}
