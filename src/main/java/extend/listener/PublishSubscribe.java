package extend.listener;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 田奇杭
 * @Description 发布订阅功能
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
    private final Map<String, Set<Thread>> waitThreadMap = new ConcurrentHashMap<>();


    @Override
    public void onMessage(Message message, byte[] pattern) {
        log.info("监听到的消息为：{}", JSON.toJSONString(message));
    }


    public boolean addWaitThread(Thread waitThread) {

        return true;
    }
}
