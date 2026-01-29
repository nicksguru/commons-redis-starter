package guru.nicks.commons.cucumber;

import guru.nicks.commons.serializer.OneNioSerializer;
import guru.nicks.commons.utils.UuidUtils;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.UUID;

import static guru.nicks.commons.cache.domain.CacheConstants.PERSISTENT_CACHE_MANAGER_PREFIX;
import static guru.nicks.commons.cache.domain.CacheConstants.TOPIC_DELIMITER;
import static guru.nicks.commons.cache.domain.CacheConstants.TTL_1HR;

/**
 * Called from integration tests to generate values with and without caching.
 * <p>
 * NOTE: {@link UUID} is not cacheable because, as per {@link OneNioSerializer}, it's not {@link Serializable}.
 */
@Component
public class RedisCacheTestComponent {

    @Cacheable(
            cacheNames = "test" + TOPIC_DELIMITER + "cache",
            cacheManager = PERSISTENT_CACHE_MANAGER_PREFIX + TTL_1HR)
    public String generateCacheableString() {
        return UuidUtils.generateUuidV4().toString();
    }

    public String generateUncacheableString() {
        return UuidUtils.generateUuidV4().toString();
    }

}
