package guru.nicks.commons.redis.impl;

import guru.nicks.commons.auth.domain.BlockedTokenHash;
import guru.nicks.commons.redis.repository.BlockedTokenRepository;
import guru.nicks.commons.service.BlockedJwtService;
import guru.nicks.commons.utils.AuthUtils;

import am.ik.yavi.meta.ConstraintArguments;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.SignedJWT;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.keyvalue.repository.KeyValueRepository;

import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotBlank;

/**
 * Redis-based implementation.
 */
@RequiredArgsConstructor
public class BlockedJwtServiceImpl implements BlockedJwtService {

    // create caffeine cache for 'token blocked' status
    // so that multiple apps don't block the same JWT simultaneously
    private final Cache<String, Boolean> isJwtBlockedCache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofMinutes(10))
            .build();

    @NonNull // Lombok creates runtime nullness check for this own annotation only
    private final BlockedTokenRepository blockedTokenRepository;

    @ConstraintArguments
    @Override
    public void blockJwt(String jwtAsString) {
        checkNotBlank(jwtAsString, _BlockedJwtServiceImplBlockJwtArgumentsMeta.JWTASSTRING.name());

        Date expiresAt;
        try {
            // parsing is NOT validation
            JWT jwt = SignedJWT.parse(jwtAsString);
            expiresAt = jwt.getJWTClaimsSet().getExpirationTime();
        } catch (ParseException e) {
            throw new IllegalArgumentException("Failed to parse JWT: " + e.getMessage(), e);
        }

        String cacheKey = generateCacheKey(jwtAsString);

        BlockedTokenHash blockedTokenHash = BlockedTokenHash.builder()
                .tokenChecksum(generateCacheKey(jwtAsString))
                // add some extra time to account for JWT expiration time precision
                .timeToLiveSec(Duration
                        .between(Instant.now(), expiresAt.toInstant())
                        .toSeconds() + 60)
                .build();
        blockedTokenHash = blockedTokenRepository.save(blockedTokenHash);
        isJwtBlockedCache.put(cacheKey, true);
    }

    /**
     * Caches the result for 10 minutes in memory to reduce the load on Redis and make authentication faster
     * ({@link KeyValueRepository#existsById(Object)} takes almost 50ms according to 99 percentile metrics).
     * <p>
     * WARNING: caching in memory means each app maintains its own cache (this is intentional for performance reasons).
     */
    @Override
    public boolean isJwtBlocked(String jwtAsString) {
        String cacheKey = generateCacheKey(jwtAsString);

        // theoretically, Caffeine may return null (for a missing key), but in this use case, it should not
        return Boolean.TRUE.equals(
                isJwtBlockedCache.get(cacheKey, blockedTokenRepository::existsById));
    }

    /**
     * Generates a cache key for the given JWT string by calling
     * {@link AuthUtils#calculateAccessTokenChecksum(String)}.
     *
     * @param jwtAsString The JWT token as a string.
     * @return cache key
     */
    protected String generateCacheKey(String jwtAsString) {
        return AuthUtils.calculateAccessTokenChecksum(jwtAsString);
    }

}
