package guru.nicks.commons.redis.repository;

import guru.nicks.commons.auth.domain.BlockedTokenHash;

import org.springframework.data.keyvalue.repository.KeyValueRepository;

public interface BlockedTokenRepository extends KeyValueRepository<BlockedTokenHash, String> {
}
