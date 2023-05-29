package extend.controller;

import extend.annotation.DistributedLock;
import extend.annotation.DistributedLockKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author 田奇杭
 * @Description 分布式锁切面测试类
 * @Date 2023/5/28 0:09
 */
@Slf4j
@Service
public class AspectServiceImpl implements AspectService{

    @Override
    @DistributedLock(leaseTime = 1000000, waitTime = 60000)
    public void test(@DistributedLockKey String key) {
        Thread currentThread = Thread.currentThread();
        log.info("当前线程为：{}", currentThread.getName());
    }
}
