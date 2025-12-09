package guru.nicks.commons.redis.config;

import guru.nicks.commons.redis.RedisSerializerAdapterImpl;
import guru.nicks.commons.redis.domain.RedisProperties;
import guru.nicks.commons.redis.impl.BlockedJwtServiceImpl;
import guru.nicks.commons.redis.impl.DistributedLockServiceImpl;
import guru.nicks.commons.redis.repository.BlockedTokenRepository;
import guru.nicks.commons.serializer.NativeJavaSerializer;
import guru.nicks.commons.serializer.OneNioSerializer;
import guru.nicks.commons.service.BlockedJwtService;
import guru.nicks.commons.service.DistributedLockService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SslVerificationMode;
import org.redisson.spring.starter.RedissonAutoConfiguration;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.serializer.DefaultSerializer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.io.Externalizable;
import java.io.Serializable;

/**
 * Why Redisson? - See <a href="https://redisson.org/feature-comparison-redisson-vs-jedis.html">here</a>.
 * <p>
 * The serializer is {@link OneNioSerializer} because it's performant and handles various edge cases. It, just like
 * {@link DefaultSerializer}, requires the payload to be {@link Serializable} (or {@link Externalizable}).
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RedisProperties.class)
@Slf4j
@RequiredArgsConstructor
public class CommonsRedisAutoConfiguration {

    // DI
    private final RedisProperties redisProperties;

    /**
     * Creates {@link BlockedJwtService} bean if it's not already present.
     */
    @ConditionalOnMissingBean(BlockedJwtService.class)
    @Bean
    public BlockedJwtService blockedJwtService(BlockedTokenRepository blockedTokenRepository) {
        log.debug("Building {} bean", BlockedJwtService.class.getSimpleName());
        return new BlockedJwtServiceImpl(blockedTokenRepository);
    }

    /**
     * Creates {@link DistributedLockService} bean if it's not already present.
     */
    @ConditionalOnMissingBean(DistributedLockService.class)
    @Bean
    public DistributedLockService distributedLockService(RedissonClient redissonClient) {
        log.debug("Building {} bean", DistributedLockService.class.getSimpleName());
        return new DistributedLockServiceImpl(redissonClient);
    }

    /**
     * Creates {@link RedisSerializer} bean if it's not already present (specifically, a
     * {@link GenericJackson2JsonRedisSerializer}).
     */
    @ConditionalOnMissingBean(RedisSerializer.class)
    @Bean
    public RedisSerializer<Object> redisSerializer(NativeJavaSerializer nativeJavaSerializer) {
        log.debug("Building {} bean", RedisSerializer.class.getSimpleName());
        return new RedisSerializerAdapterImpl<>(nativeJavaSerializer);
        //return RedisSerializer.java(); //new CustomRedisSerializer();
    }

    /**
     * Beans of this class are called from {@link RedissonAutoConfiguration#redisson()} which creates own {@link Config}
     * instance (not a bean) with just a couple of basic parameters and uses it for communicating with Redisson. That
     * is, custom {@link Config} bean is never honored, which leads to the necessity of updating Spring Boot's config
     * via this customizer.
     *
     * @return customizer
     */
    @Bean
    public RedissonAutoConfigurationCustomizer commonsRedissonAutoConfigurationCustomizer() {
        log.debug("Building {} bean", RedissonAutoConfigurationCustomizer.class.getSimpleName());
        return this::populateRedissonConfig;
    }

    /**
     * Populates Redisson config instance with actual values.
     *
     * @param redissonConfig config instance to populate
     * @return same as argument
     */
    private Config populateRedissonConfig(Config redissonConfig) {
        // avoid accidentally exposing passwords in logs
        log.info("Connecting to Redis at '{}://{}:{}' (database: '{}')",
                redisProperties.getScheme(), redisProperties.getHost(), redisProperties.getPort(),
                redisProperties.getDatabase());

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

    /**
     * Can't just use a custom {@link ObjectMapper} - {@link GenericJackson2JsonRedisSerializer} sets up Jackson to
     * store class names as property names, which is not trivial to do.
     */
    public static class CustomJsonSerializer extends GenericJackson2JsonRedisSerializer {

        public CustomJsonSerializer() {
            super();
            // process Java 8 dates
            getObjectMapper().registerModule(new JavaTimeModule());
        }

    }

}
