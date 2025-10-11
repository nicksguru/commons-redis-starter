package guru.nicks.cucumber;

import guru.nicks.cucumber.config.TestDefaultCacheManager;
import guru.nicks.cucumber.world.TextWorld;
import guru.nicks.redis.RedisSerializerAdapter;
import guru.nicks.redis.config.RedisCacheConfig;
import guru.nicks.redis.config.RedisConfig;
import guru.nicks.serializer.OneNioSerializer;
import guru.nicks.test.RedisContainerRunner;

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

        RedisConfig.class, RedisCacheConfig.class, OneNioSerializer.class, RedisSerializerAdapter.class,
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
