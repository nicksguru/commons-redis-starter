package guru.nicks.redis.config;

import guru.nicks.cache.domain.CacheConstants;
import guru.nicks.cache.domain.CacheProperties;
import guru.nicks.utils.TimeUtils;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;
import java.util.Collection;
import java.util.function.IntFunction;

/**
 * Creates Redis caches with various TTLs according to {@link CacheProperties#getDurations()}. Additionally, put/evict
 * operations are synchronized with ongoing Spring-managed transactions.
 * <p>
 * WARNING: only results of public bean methods can be cached with {@link Cacheable @Cacheable} (because of proxies).
 *
 * @see #createRedisCacheManager(String, Duration)
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CacheProperties.class)
@EnableCaching // to honor @Cacheable
@RequiredArgsConstructor
@Slf4j
public class RedisCacheConfig {

    /**
     * {@link GenericJackson2JsonRedisSerializer} throws error 'Type id handling not implemented for type
     * {@code Object}' for immutable collections, therefore native Java serializers are used. They create
     * non-human-readable data but cover polymorphism and all Java classes reliably, which is crucial for
     * {@link Cacheable @Cacheable} to work seamlessly with any method results. Also, such cache is internal and not
     * meant to be accessed from other languages (unlike DTOs which are usually JSON).
     */
    private final RedisSerializer<?> redisSerializer;

    // DI
    private final CacheProperties cacheProperties;
    private final RedisConnectionFactory redisConnectionFactory;
    private final GenericApplicationContext appContext;

    /**
     * @see #createRedisCacheConfig(Duration)
     */
    private RedisSerializationContext.SerializationPair<?> valueSerializer;

    /**
     * Spring-native setting.
     */
    @Value("${spring.cache.redis.key-prefix}")
    private String keyPrefix;

    @PostConstruct
    public void init() {
        // let @Cacheable without any cache manager specified work as it does by default, in memory
        log.info("Default cache manager (@Cacheable without TTL) remains as it was, in-memory");

        createRedisCacheManagers(cacheProperties.getDurations().getMinutes(), Duration::ofMinutes);
        createRedisCacheManagers(cacheProperties.getDurations().getHours(), Duration::ofHours);
        createRedisCacheManagers(cacheProperties.getDurations().getDays(), Duration::ofDays);
    }

    /**
     * Creates cache managers based on {@link CacheProperties.CacheDefinition}.
     *
     * @param cacheDefinitions cache definitions
     * @param durationCreator  creates {@link Duration} out of {@link CacheProperties.CacheDefinition#getValue()}
     */
    private void createRedisCacheManagers(Collection<CacheProperties.CacheDefinition> cacheDefinitions,
            IntFunction<Duration> durationCreator) {

        for (var cacheDefinition : cacheDefinitions) {
            var cacheManagerNameBuilder = new StringBuilder()
                    .append(CacheConstants.PERSISTENT_CACHE_MANAGER_PREFIX)
                    .append(cacheDefinition.getValue());

            if (StringUtils.isNotBlank(cacheDefinition.getSuffix())) {
                cacheManagerNameBuilder.append(cacheDefinition.getSuffix());
            }

            createRedisCacheManager(cacheManagerNameBuilder.toString(),
                    durationCreator.apply(cacheDefinition.getValue()));
        }
    }

    /**
     * Creates cache manager.
     *
     * @param cacheManagerName cache manager name
     * @param ttl              cache TTL
     */
    private void createRedisCacheManager(String cacheManagerName, Duration ttl) {
        log.info("Creating Redis cache manager with TTL of {}, usage: "
                        + "@Cacheable(cacheNames = \"someCache\", key = \"#someArg\", cacheManager = \"{}\")",
                TimeUtils.humanFormatDuration(ttl), cacheManagerName);
        var cacheConfig = createRedisCacheConfig(ttl);
        registerRedisCacheManagerBean(cacheManagerName, cacheConfig);
    }

    private RedisCacheConfiguration createRedisCacheConfig(Duration ttl) {
        if (valueSerializer == null) {
            valueSerializer = RedisSerializationContext.SerializationPair.fromSerializer(redisSerializer);
        }

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .prefixCacheNameWith(keyPrefix)
                .serializeValuesWith(valueSerializer);
    }

    private void registerRedisCacheManagerBean(String beanName, RedisCacheConfiguration cacheConfig,
            BeanDefinitionCustomizer... customizers) {
        var builder = RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(cacheConfig);

        // synchronize put/evict operations with ongoing Spring-managed transactions
        if (cacheProperties.isTransactionAware()) {
            log.warn("Redis cache is now transaction-aware - counter-intuitive side effects may arise");
            builder.transactionAware();
        }

        RedisCacheManager cacheManager = builder.build();
        appContext.registerBean(beanName, CacheManager.class, () -> cacheManager, customizers);
    }

}
