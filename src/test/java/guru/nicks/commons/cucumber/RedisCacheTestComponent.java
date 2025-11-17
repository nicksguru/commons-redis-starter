package guru.nicks.commons.cucumber;

import guru.nicks.commons.cache.domain.CacheConstants;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Called from integration tests to generate values with and without caching.
 */
@Component
public class RedisCacheTestComponent {

    @Cacheable(cacheNames = "test::uuid",
            cacheManager = CacheConstants.PERSISTENT_CACHE_MANAGER_PREFIX + CacheConstants.TTL_1HR)
    public UUID generateCacheableUUID() {
        return UUID.randomUUID();
    }

    public UUID generateUncacheableUUID() {
        return UUID.randomUUID();
    }

}
