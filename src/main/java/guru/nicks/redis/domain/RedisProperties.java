package guru.nicks.redis.domain;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.extern.jackson.Jacksonized;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Redis connection properties.
 */
@ConfigurationProperties(prefix = "spring.redis")
@Validated
// immutability
@Value
@NonFinal // needed for CGLIB to bind property values (nested classes don't need this)
@Jacksonized
@Builder(toBuilder = true)
public class RedisProperties {

    @NotBlank
    String host;

    @Positive
    int port;

    /**
     * Usually between 0 and 16, inclusive. Default database is always 0, yet this property is non-nullable to make the
     * choice explicit.
     */
    @Min(0)
    @NotNull
    Integer database;

    /**
     * Nullable because Redis isn't always password-protected. Passing something when nothing is expected by server
     * causes an error.
     */
    @ToString.Exclude
    String password;

    /**
     * {@code redis} - no SSL, {@code rediss} - with SSL
     */
    @NotBlank
    String scheme;

    boolean trustAnyCertificate;

    /**
     * Default (24) is rejected by Redis when there are many microservices and each one wants that much.
     */
    @Min(1)
    int connectionMinimumIdleSize;

}
