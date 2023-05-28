package extend.controller;

import extend.annotation.DistributedLock;
import extend.annotation.DistributedLockKey;
import org.springframework.stereotype.Service;

/**
 * @author 田奇杭
 * @Description 分布式锁切面测试类
 * @Date 2023/5/28 0:09
 */
@Service
public class AspectServiceImpl implements AspectService{

    @Override
    @DistributedLock
    public void test(@DistributedLockKey String key) {
        System.out.println("很开心哦");
    }
}
