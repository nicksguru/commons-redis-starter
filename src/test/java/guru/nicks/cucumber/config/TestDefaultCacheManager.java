package guru.nicks.cucumber.config;

import guru.nicks.cache.domain.CacheConstants;

import org.springframework.cache.Cache;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * The starter creates multiple cache managers (one for each duration), and there's no default one (which is usually
 * memory-based). Without it, Spring Boot fails to start.
 */
@Component(CacheConstants.DEFAULT_CACHE_MANAGER_BEAN)
@Primary
public class TestDefaultCacheManager implements org.springframework.cache.CacheManager {

    @Override
    public Cache getCache(String name) {
        return null;
    }

    @Override
    public Collection<String> getCacheNames() {
        return List.of();
    }

}
