package guru.nicks.commons.cucumber;

import guru.nicks.commons.cucumber.config.TestDefaultCacheManager;
import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.redis.RedisSerializerAdapterImpl;
import guru.nicks.commons.redis.config.CommonsRedisAutoConfiguration;
import guru.nicks.commons.redis.config.CommonsRedisCacheConfig;
import guru.nicks.commons.serializer.OneNioSerializer;
import guru.nicks.commons.test.RedisContainerRunner;

import io.cucumber.spring.CucumberContextConfiguration;
import org.redisson.spring.starter.RedissonAutoConfigurationV2;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

/**
 * Initializes Spring Context shared by all scenarios. Mocking is done inside step definition classes to let them
 * program a different behavior. However, purely default mocks can be declared here (using annotations), but remember to
 * not alter their behavior in step classes.
 */
@CucumberContextConfiguration
@ContextConfiguration(classes = {
        // scenario-scoped states
        TextWorld.class,

        CommonsRedisAutoConfiguration.class, CommonsRedisCacheConfig.class, OneNioSerializer.class,
        RedisSerializerAdapterImpl.class,
        RedisCacheTestComponent.class,

        RedissonAutoConfigurationV2.class, TestDefaultCacheManager.class
}, initializers = RedisContainerRunner.class)
@ActiveProfiles("local")
@TestPropertySource(properties = {
        //"logging.level.root=DEBUG",

        // needed for component scanners, such as Spring Data
        "app.rootPackage=guru.nicks",

        // each cache manager corresponds to a certain TTL
        "cache.inMemory.maxEntriesPerCacheManager=50000",
})
public class CucumberBootstrap {
}
