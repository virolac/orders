package io.github.virolac.orders.product;

import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration(proxyBeanMethods = false)
@EnableCaching
public class ProductCacheConfig {
    public static final String PRODUCTS_CACHE = "products";

    @Bean
    public RedisCacheManagerBuilderCustomizer productsCacheCustomizer() {
        RedisCacheConfiguration config = RedisCacheConfiguration
                .defaultCacheConfig()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new JacksonJsonRedisSerializer<>(ProductResponse.class)));

        return builder -> builder.withCacheConfiguration(PRODUCTS_CACHE, config);
    }
}
