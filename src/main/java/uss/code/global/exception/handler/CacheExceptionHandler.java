package uss.code.global.exception.handler;

import lombok.extern.log4j.Log4j2;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

@Log4j2
public class CacheExceptionHandler implements CacheErrorHandler {

    @Override
    public void handleCacheGetError(
            final RuntimeException exception,
            final Cache cache,
            final Object key
    ) {
        log.warn("Cache get failed, falling back to source. cache={}, key={}, message={}", cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCachePutError(
            final RuntimeException exception,
            final Cache cache,
            final Object key,
            final Object value
    ) {
        log.warn("Cache put failed. cache={}, key={}, message={}", cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCacheEvictError(
            final RuntimeException exception,
            final Cache cache,
            final Object key
    ) {
        log.warn("Cache evict failed. cache={}, key={}, message={}", cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCacheClearError(
            final RuntimeException exception,
            final Cache cache
    ) {
        log.warn("Cache clear failed. cache={}, message={}", cache.getName(), exception.getMessage());
    }
}
