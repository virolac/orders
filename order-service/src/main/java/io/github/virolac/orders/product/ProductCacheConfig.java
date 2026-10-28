package io.github.virolac.orders.product;

import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@EnableCaching
public class ProductCacheConfig {
    public static final String PRODUCTS_CACHE = "products";
    public static final Duration PRODUCT_TTL = Duration.ofMinutes(10);

    @Bean
    public RedisCacheManagerBuilderCustomizer productsCacheCustomizer(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(PRODUCT_TTL)
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new JacksonJsonRedisSerializer<>(ProductResponse.class)));

        // Redis caches write asynchronously by default since Spring Data Redis 4, so an
        // eviction may land after deleteProduct returns and a following read could still
        // see the deleted product. Immediate writes wait for Redis to confirm each write.
        RedisCacheWriter cacheWriter = RedisCacheWriter.create(
                connectionFactory, RedisCacheWriter.RedisCacheWriterConfigurer::immediateWrites);

        return builder -> builder
                .withCacheConfiguration(PRODUCTS_CACHE, config)
                .transactionAware()
                .cacheWriter(cacheWriter);
    }
}
