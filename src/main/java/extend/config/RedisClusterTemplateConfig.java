package extend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * @author 田奇杭
 * @Description
 * @Date 2022/8/29 23:55
 */
@Configuration
public class RedisClusterTemplateConfig {

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, String> redisConfig = new RedisTemplate<>();
        redisConfig.setConnectionFactory(redisConnectionFactory);
        // 使用 GenericFastJsonRedisSerializer 替换默认序列化
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        // 设置key和value的序列化规则
        redisConfig.setKeySerializer(stringRedisSerializer);
        redisConfig.setValueSerializer(stringRedisSerializer);
        // 设置hashKey和hashValue的序列化规则
        redisConfig.setHashKeySerializer(stringRedisSerializer);
        redisConfig.setHashValueSerializer(stringRedisSerializer);
        // 设置支持事物
        redisConfig.setEnableTransactionSupport(true);
        redisConfig.afterPropertiesSet();
        return redisConfig;
    }

}
