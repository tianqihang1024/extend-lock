package extend.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author 田奇杭
 * @Description 分布式锁Key注解，标注那个参数是分布式锁的关键字
 * @Date 2023/5/28 22:03
 */
@Documented
@Target({PARAMETER})
@Retention(RUNTIME)
public @interface DistributedLockKey {


}
