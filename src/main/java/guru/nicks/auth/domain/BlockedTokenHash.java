package guru.nicks.auth.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.annotation.Id;
import org.springframework.data.keyvalue.annotation.KeySpace;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

/**
 * Access token (presumably JWT) that has been blocked.
 */
@RedisHash
@KeySpace("BlockedTokenHash")
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
    private long timeToLiveSec;

}
