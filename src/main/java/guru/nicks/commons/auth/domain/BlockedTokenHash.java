package guru.nicks.commons.auth.domain;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

/**
 * Access token (presumably a JWT) that has been blocked.
 */
@RedisHash("blocked-access-token")
//
@Data
@NoArgsConstructor
@AllArgsConstructor
//
@Jacksonized
@Builder(toBuilder = true)
public class BlockedTokenHash {

    @Id
    private String tokenChecksum;

    /**
     * Seconds until expiration.
     */
    @TimeToLive
    @Min(0)
    @NotNull
    private Long timeToLiveSec;

}
