package guru.nicks.commons.redis.config;

import guru.nicks.commons.cache.domain.CacheConstants;
import guru.nicks.commons.cache.domain.CacheProperties;
import guru.nicks.commons.utils.text.TimeUtils;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Collection;
import java.util.function.IntFunction;

/**
 * Creates Redis caches with various TTLs according to {@link CacheProperties#getDurations()}. Put/evict operations are
 * synchronized with ongoing Spring-managed transactions if {@link CacheProperties#isTransactionAware()} is {@code true}
 * (see description of the above flag for caveats).
 * <p>
 * Also, creates a {@link RedisTemplate} bean with string keys, injectable as {@code RedisTemplate<String, Object>}.
 * <p>
 * The cache manager names are logged and can be used programmatically as well:
 * <pre>
 *  &#64;Qualifier("cacheManagerName")
 *  private final CacheManager cacheManager;
 *  ...
 *  cacheManager.getCache("myCache").put("myKey", "myValue");
 * </pre>
 * <p>
 * WARNING: only results of public bean methods can be cached with {@link Cacheable @Cacheable} (because of proxies).
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CacheProperties.class)
@EnableCaching // to honor @Cacheable
@RequiredArgsConstructor
@Slf4j
public class CommonsRedisCacheConfig {

    // DI
    private final CacheProperties cacheProperties;
    private final RedisSerializer<?> redisSerializer;
    private final RedisConnectionFactory redisConnectionFactory;
    private final GenericApplicationContext appContext;

    /**
     * @see #createRedisCacheConfig(Duration)
     */
    private RedisSerializationContext.SerializationPair<?> valueSerializer;

    /**
     * Spring-native setting - optional prefix.
     */
    @Value("${spring.cache.redis.key-prefix:}")
    private String keyPrefix;

    @PostConstruct
    private void init() {
        // let @Cacheable without any cache manager specified work as it does by default, in memory
        log.info("Default cache manager (@Cacheable without TTL) remains as it was, in-memory");

        createRedisCacheManagers(cacheProperties.getDurations().getMinutes(), Duration::ofMinutes);
        createRedisCacheManagers(cacheProperties.getDurations().getHours(), Duration::ofHours);
        createRedisCacheManagers(cacheProperties.getDurations().getDays(), Duration::ofDays);
    }

    @ConditionalOnMissingBean(RedisTemplate.class)
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory, RedisSerializer<?> redisSerializer) {
        var template = new RedisTemplate<String, Object>();
        template.setConnectionFactory(connectionFactory);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(redisSerializer);

        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(redisSerializer);

        return template;
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

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .serializeValuesWith(valueSerializer);

        if (StringUtils.isNotBlank(keyPrefix)) {
            config.prefixCacheNameWith(keyPrefix);
        }

        return config;
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
