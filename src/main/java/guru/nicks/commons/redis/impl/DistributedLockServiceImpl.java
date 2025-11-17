package guru.nicks.commons.redis.impl;

import guru.nicks.commons.service.DistributedLockService;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * {@link RLock}-based implementation.
 */
@RequiredArgsConstructor
public class DistributedLockServiceImpl implements DistributedLockService {

    // DI
    private final RedissonClient redissonClient;

    @Override
    public <T> T withExclusiveLock(String lockName, Duration lockTtl, Supplier<T> code) {
        RLock lock = redissonClient.getFairLock(lockName);

        try {
            lock.lock(lockTtl.toMillis(), TimeUnit.MILLISECONDS);
            return code.get();
        } finally {
            lock.unlock();
        }
    }

}
