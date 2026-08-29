package uss.code.global.config;

import io.lettuce.core.ClientOptions.DisconnectedBehavior;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.boot.data.redis.autoconfigure.LettuceClientOptionsBuilderCustomizer;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import uss.code.course.dto.common.CachedMajorCourses;
import uss.code.course.infra.CourseCacheLoader;
import uss.code.global.exception.handler.CacheExceptionHandler;

import java.util.Objects;

@Configuration
@EnableCaching
public class RedisCacheConfig implements CachingConfigurer {

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheExceptionHandler();
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer majorCoursesCacheCustomizer() {
        return builder -> builder.withCacheConfiguration(
                CourseCacheLoader.MAJOR_COURSES,
                builder.cacheDefaults().serializeValuesWith(SerializationPair.fromSerializer(
                        new JacksonJsonRedisSerializer<>(CachedMajorCourses.class)))
        );
    }

    @Bean
    public LettuceClientOptionsBuilderCustomizer rejectCommandsWhileDisconnected() {
        return builder -> builder.disconnectedBehavior(DisconnectedBehavior.REJECT_COMMANDS);
    }

    @Bean
    public ApplicationRunner cacheFlusher(final CacheManager cacheManager) {
        return args -> cacheManager.getCacheNames()
                .forEach(name -> Objects.requireNonNull(cacheManager.getCache(name)).clear());
    }
}
