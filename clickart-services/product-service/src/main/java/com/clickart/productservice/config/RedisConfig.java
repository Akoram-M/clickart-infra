package com.clickart.productservice.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class RedisConfig {

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {

        ObjectMapper objMapper = new ObjectMapper();

        // 1. Support Java 8 LocalDateTime
        objMapper.registerModule(new JavaTimeModule());

        // 2. Format dates as human-readable strings, not raw timestamps
        objMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 3. Embed class type into JSON so Spring can deserialize List<Product> correctly
        objMapper.activateDefaultTyping(
                objMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objMapper);

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5)) // Forces cache to die after 5 minutes
                .disableCachingNullValues()
                // INSTRUCT SPRING TO USE JSON SERIALIZATION
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
    }
}
