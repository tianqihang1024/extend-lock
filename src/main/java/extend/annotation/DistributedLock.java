package extend.annotation;

import extend.enums.DistributedLockTypeEnum;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author 田奇杭
 * @Description 分布式锁注解
 * @Date 2023/5/28 22:03
 */
@Documented
@Target({METHOD})
@Retention(RUNTIME)
public @interface DistributedLock {

    /**
     * 分布式锁的等待时间
     *
     * @return 锁等待时间
     */
    long waitTime() default 6000L;

    /**
     * 分布式锁的持续时间
     *
     * @return 锁持续时间
     */
    long leaseTime() default 30000L;

    /**
     * 分布式锁的时间单位
     *
     * @return 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;

    /**
     * 分布式锁模式
     *
     * @return 分布式锁类型
     */
    DistributedLockTypeEnum type() default DistributedLockTypeEnum.ORDINARY;

}
