package extend.controller;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author 田奇杭
 * @Description 分布式锁测试控制层
 * @Date 2023/5/14 1:35
 */
@Slf4j
@RestController
@RequestMapping("/lock/")
public class LockController {

    /**
     * 监听释放锁主题
     */
    private static final String UN_LOCK_TOPIC = "UN_LOCK_TOPIC";
    String string = "return redis.call('ZRANGE', KEYS[1], 1, 2);";
    @Resource
    private RedisTemplate<String, String> redisTemplate;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private AspectService aspectService;

    /**
     * 创建一个发布订阅模式的订阅者，监听所有 channel，监听者方法的入参是锁的名称
     * 所有线程
     */
    @RequestMapping("lock")
    public void lock(String key) {
        List<Thread> threadList = new ArrayList<>(10);
        for (int i = 0; i < 5; i++) {
            Thread thread = new Thread(() -> aspectService.test(key));
            thread.setName("thread:" + i);
            threadList.add(thread);
        }
        log.info("开始抢占线程");
        for (Thread thread : threadList) {
            thread.start();
        }
        log.info("5次睡眠结束");
    }

    @RequestMapping(value = "test")
    public Object test(String key) {


        DefaultRedisScript<Object> redisScript = new DefaultRedisScript<>(string, Object.class);
        Object value = redisTemplate.execute(redisScript, Collections.singletonList(key));


        redisTemplate.convertAndSend("channel", "");

        RedissonClient redissonClient = Redisson.create();
        RLock lock = redissonClient.getLock("");
        try {
            lock.tryLock(0L, 0L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {

        }

        return value;
    }
}
