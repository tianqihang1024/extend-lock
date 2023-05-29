package extend.aspect;

import extend.annotation.DistributedLock;
import extend.annotation.DistributedLockKey;
import extend.enums.DistributedLockTypeEnum;
import extend.lock.AbstractDistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author 田奇杭
 * @Description 分布式锁注解切面类
 * @Date 2023/5/9 21:47
 */
@Slf4j
@Aspect
@Component
public class DistributedLockAspect {

    /**
     * 拦截所有标注 @DistributedLock 的方法
     */
    @Pointcut("@annotation(extend.annotation.DistributedLock)")
    private void targetMethod() {
        // 这是切入点的声明方法
    }

    /**
     * 分布式锁实现类Map key:实现类名称 value:实现类对象
     */
    @Resource
    private Map<String, AbstractDistributedLock> distributedLockMap;

    /**
     * 环绕通知：灵活自由的在目标方法中切入代码
     */
    @Around("targetMethod()")
    public Object around(ProceedingJoinPoint joinPoint) {

        // 分布式锁注解
        DistributedLock distributedLock = getDistributedLock(joinPoint);

        // 获取关键字
        String keyword = getKeyword(joinPoint);

        // 非空检查
        if (distributedLock == null || keyword == null)
            return null;

        // 获取分布式锁对象
        DistributedLockTypeEnum type = distributedLock.type();
        AbstractDistributedLock abstractDistributedLock = distributedLockMap.get(type.getDistributedLockName());

        boolean flag = abstractDistributedLock.tryLock(keyword, distributedLock.waitTime(), distributedLock.leaseTime(), distributedLock.timeUnit());
        try {
            if (flag)
                // 执行源方法
                return joinPoint.proceed();
        } catch (Throwable e) {
            log.error("DistributedLockAspect.around tryLock fail keyword:{} distributedLock:{}", keyword, distributedLock, e);
        } finally {
            if (flag)
                abstractDistributedLock.unLock(keyword, distributedLock.leaseTime(), distributedLock.timeUnit());
        }
        return null;
    }

    /**
     * 获取方法上方的分布式锁注解
     *
     * @param joinPoint 连接点
     * @return 分布式锁注解
     */
    private DistributedLock getDistributedLock(ProceedingJoinPoint joinPoint) {

        // 当前执行的方法
        Method objMethod = getMethod(joinPoint);

        if (objMethod == null)
            return null;

        for (Annotation annotation : objMethod.getAnnotations()) {
            if (annotation instanceof DistributedLock)
                return (DistributedLock) annotation;
        }
        return null;
    }

    /**
     * 获取关键字
     *
     * @param joinPoint 连接点
     * @return 关键字
     */
    private String getKeyword(ProceedingJoinPoint joinPoint) {

        // 获取方法传入参数
        Object[] params = joinPoint.getArgs();

        // 当前执行的方法
        Method objMethod = getMethod(joinPoint);

        // 非空检查
        if (params == null || objMethod == null)
            return null;

        // 获取方法上的注解
        Annotation[][] annotations = objMethod.getParameterAnnotations();

        // 循环参数列表
        for (int i = 0; i < params.length; i++) {
            // 查看每个参数的第一个注解是否为 DistributedLockKey
            if (annotations[i][0] instanceof DistributedLockKey)
                // 返回关键字
                return String.valueOf(params[i]);
        }
        return null;
    }

    /**
     * 获取当前方法
     *
     * @param joinPoint 连接点
     * @return 当前方法
     */
    private Method getMethod(ProceedingJoinPoint joinPoint) {

        // 获取目标方法的名称
        String methodName = joinPoint.getSignature().getName();

        // 反射获取目标类
        Class<?> targetClass = joinPoint.getTarget().getClass();

        // 拿到方法对应的参数类型
        Class<?>[] parameterTypes = ((MethodSignature) joinPoint.getSignature()).getParameterTypes();

        try {
            // 根据类、方法、参数类型（重载）获取到方法的具体信息
            return targetClass.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            log.error("DistributedLockAspect.getMethod fail methodName:{}", methodName, e);
        }
        return null;
    }


}
