package guru.nicks.commons.redis.impl;

import guru.nicks.commons.auth.domain.BlockedTokenHash;
import guru.nicks.commons.exception.http.ForbiddenException;
import guru.nicks.commons.redis.repository.BlockedTokenRepository;
import guru.nicks.commons.service.BlockedJwtService;
import guru.nicks.commons.utils.auth.AuthUtils;

import am.ik.yavi.meta.ConstraintArguments;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.keyvalue.repository.KeyValueRepository;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;

import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotNull;

/**
 * Redis-based implementation.
 */
@RequiredArgsConstructor
public class BlockedJwtServiceImpl implements BlockedJwtService {

    public static final int IS_JWT_BLOCKED_CACHE_TTL_MINUTES = 10;

    /**
     * @see #isJwtBlocked(Jwt)
     */
    private final Cache<String, Boolean> isJwtBlockedCache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofMinutes(IS_JWT_BLOCKED_CACHE_TTL_MINUTES))
            .build();

    @NonNull // Lombok creates runtime nullness check for this own annotation only
    private final BlockedTokenRepository blockedTokenRepository;

    @Override
    public <T> T ifBelongsToUser(Jwt jwt, String userId, Function<? super Jwt, T> mapper) {
        checkNotNull(jwt, "jwt");

        if (!userId.equals(jwt.getSubject())) {
            throw new ForbiddenException("Auth token not owned by user");
        }

        return mapper.apply(jwt);
    }

    @ConstraintArguments
    @Override
    public void blockJwt(Jwt jwt) {
        checkNotNull(jwt, _BlockedJwtServiceImplBlockJwtArgumentsMeta.JWT.name());

        Instant expiresAt = jwt.getExpiresAt();
        String cacheKey = generateCacheKey(jwt);

        BlockedTokenHash blockedTokenHash = BlockedTokenHash.builder()
                .tokenChecksum(cacheKey)
                // add some extra time to account for JWT expiration time precision
                .timeToLiveSec(Duration
                        .between(Instant.now(), expiresAt)
                        .toSeconds() + 60)
                .build();
        blockedTokenRepository.save(blockedTokenHash);
        isJwtBlockedCache.put(cacheKey, true);
    }

    /**
     * Caches the result for {@value #IS_JWT_BLOCKED_CACHE_TTL_MINUTES} minutes in memory to reduce the load on Redis
     * and make authentication faster ({@link KeyValueRepository#existsById(Object)} takes almost 50ms according to 99
     * percentile metrics).
     * <p>
     * WARNING: caching in memory means each app maintains its own cache (this is intentional for performance reasons).
     */
    @Override
    public boolean isJwtBlocked(Jwt jwt) {
        checkNotNull(jwt, "jwt");
        String cacheKey = generateCacheKey(jwt);

        // theoretically, Caffeine may return null (for a missing key), but in this use case, it should not
        return Boolean.TRUE.equals(
                isJwtBlockedCache.get(cacheKey, blockedTokenRepository::existsById));
    }

    /**
     * Generates a cache key for the given JWT by calling {@link AuthUtils#calculateAccessTokenChecksum(String)} on the
     * serialized token value.
     *
     * @param jwt token
     * @return cache key
     */
    protected String generateCacheKey(Jwt jwt) {
        return AuthUtils.calculateAccessTokenChecksum(jwt.getTokenValue());
    }

}
