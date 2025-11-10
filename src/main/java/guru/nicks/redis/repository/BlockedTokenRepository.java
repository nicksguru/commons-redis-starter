package guru.nicks.redis.repository;

import guru.nicks.auth.domain.BlockedTokenHash;

import org.springframework.data.keyvalue.repository.KeyValueRepository;

public interface BlockedTokenRepository extends KeyValueRepository<BlockedTokenHash, String> {
}
