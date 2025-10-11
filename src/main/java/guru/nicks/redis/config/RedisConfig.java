package guru.nicks.redis.config;

import guru.nicks.redis.domain.RedisProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.config.Config;
import org.redisson.config.SslVerificationMode;
import org.redisson.spring.starter.RedissonAutoConfiguration;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.keyvalue.repository.KeyValueRepository;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

/**
 * Redis repositories are all {@link KeyValueRepository} subtypes within {@code app.rootPackage} (set in config) - to
 * speed up component scan. Why Redisson? - See
 * <a href="https://redisson.org/feature-comparison-redisson-vs-jedis.html">here</a>.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RedisProperties.class)
@EnableRedisRepositories(basePackages = "${app.rootPackage}",
        includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = KeyValueRepository.class))
@Slf4j
@RequiredArgsConstructor
public class RedisConfig {

    // DI
    private final RedisProperties redisProperties;

    /**
     * Beans of this class are called from {@link RedissonAutoConfiguration#redisson()} which creates own {@link Config}
     * instance (not a bean) with just a couple of basic parameters and uses it for communicating with Redisson. That
     * is, custom {@link Config} bean is never honored, which leads to the necessity of updating Spring Boot's config
     * via this customizer.
     *
     * @return customizer
     */
    @Bean
    public RedissonAutoConfigurationCustomizer redissonAutoConfigurationCustomizer() {
        return this::populateRedissonConfig;
    }

    /**
     * Populates Redisson config instance with actual values.
     *
     * @param redissonConfig config instance to populate
     * @return same as argument
     */
    private Config populateRedissonConfig(Config redissonConfig) {
        log.info("Connecting to Redis: {}", redisProperties);

        if (!"rediss".equalsIgnoreCase(redisProperties.getScheme())) {
            log.warn("Redis is not SSL protected - your password and data may leak!");
        } else {
            if (redisProperties.isTrustAnyCertificate()) {
                log.warn("Any certificate presented by Redis is trusted - don't use this mode in production!");
            }
        }

        var config = redissonConfig.useSingleServer()
                .setDatabase(redisProperties.getDatabase())
                .setConnectionMinimumIdleSize(redisProperties.getConnectionMinimumIdleSize())
                .setSslVerificationMode(redisProperties.isTrustAnyCertificate()
                        ? SslVerificationMode.NONE
                        : SslVerificationMode.STRICT)
                .setKeepAlive(true)
                .setAddress(redisProperties.getScheme() + "://"
                        + redisProperties.getHost() + ":"
                        + redisProperties.getPort());

        // Redis isn't always password-protected. Passing something to server when nothing is expected causes an error.
        if (StringUtils.isNotBlank(redisProperties.getPassword())) {
            config.setPassword(redisProperties.getPassword());
        }

        return redissonConfig;
    }

}
