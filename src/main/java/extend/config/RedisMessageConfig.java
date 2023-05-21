package extend.config;

import extend.listener.PublishSubscribe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

/**
 * @author 田奇杭
 * @Description
 * @Date 2023/5/14 22:42
 */
@Slf4j
@Configuration
public class RedisMessageConfig {

    /**
     * 监听释放锁主题
     */
    private static final String UN_LOCK_TOPIC = "UN_LOCK_TOPIC";

    /**
     * 消息监听容器
     *
     * @param factory
     * @return
     */
    @Bean
    RedisMessageListenerContainer container(RedisConnectionFactory factory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        //订阅一个通道 该处的通道名是发布消息时的名称
        container.addMessageListener(catAdapter(), new PatternTopic(UN_LOCK_TOPIC));
        return container;
    }

    /**
     * 消息监听适配器，绑定消息处理器
     *
     * @return
     */
    @Bean
    MessageListenerAdapter catAdapter() {
        return new MessageListenerAdapter(new PublishSubscribe());
    }
}
