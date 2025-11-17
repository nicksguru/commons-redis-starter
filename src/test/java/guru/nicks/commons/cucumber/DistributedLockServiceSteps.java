package guru.nicks.commons.cucumber;

import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.redis.impl.DistributedLockServiceImpl;
import guru.nicks.commons.service.DistributedLockService;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RequiredArgsConstructor
public class DistributedLockServiceSteps {

    // DI
    private final TextWorld textWorld;
    private final AtomicBoolean codeExecuted = new AtomicBoolean(false);
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RLock lock;
    private AutoCloseable closeableMocks;
    private DistributedLockService distributedLockService;
    private Duration lockTtl;
    private Supplier<String> codeSupplier;
    private String result;

    @Before
    public void beforeEachScenario() {
        closeableMocks = MockitoAnnotations.openMocks(this);
        distributedLockService = new DistributedLockServiceImpl(redissonClient);
    }

    @After
    public void afterEachScenario() throws Exception {
        closeableMocks.close();
    }

    @Given("a distributed lock service is available")
    public void aDistributedLockServiceIsAvailable() {
        codeSupplier = () -> {
            codeExecuted.set(true);
            return "success";
        };
    }

    @Given("the code will throw an exception")
    public void theCodeWillThrowAnException() {
        codeSupplier = () -> {
            codeExecuted.set(true);
            throw new RuntimeException("Test exception");
        };
    }

    @When("code is executed with lock name {string} and lock TTL of {int} ms")
    public void codeIsExecutedWithLockNameAndLockTTLOfMs(String lockName, int lockTtlMs) {
        lockTtl = Duration.ofMillis(lockTtlMs);

        // setup mock for this specific lock name
        when(redissonClient.getFairLock(lockName))
                .thenReturn(lock);

        Throwable thrown = catchThrowable(() ->
                result = distributedLockService.withExclusiveLock(lockName, lockTtl, codeSupplier));
        textWorld.setLastException(thrown);
    }

    @Then("the code should be executed successfully")
    public void theCodeShouldBeExecutedSuccessfully() {
        assertThat(codeExecuted.get())
                .as("Code execution flag")
                .isTrue();

        assertThat(result)
                .as("Execution result")
                .isEqualTo("success");
    }

    @Then("the lock should be released even when an exception occurs")
    public void theLockShouldBeReleasedEvenWhenAnExceptionOccurs() {
        verify(lock).lock(lockTtl.toMillis(), TimeUnit.MILLISECONDS);
        verify(lock, never()).isHeldByCurrentThread();
        verify(lock).unlock();
    }

    @Given("a lock that fails to be acquired")
    public void aLockThatFailsToBeAcquired() {
        when(redissonClient.getFairLock("failing-lock"))
                .thenReturn(lock);

        doThrow(new RuntimeException("Failed to acquire lock"))
                .when(lock).lock(anyLong(), eq(TimeUnit.MILLISECONDS));
    }

    @When("attempting to execute code with a failing lock")
    public void attemptingToExecuteCodeWithAFailingLock() {
        Throwable thrown = catchThrowable(() ->
                distributedLockService.withExclusiveLock("failing-lock", Duration.ofMillis(1000), codeSupplier));
        textWorld.setLastException(thrown);
    }

    @Then("the unlock method should not be called")
    public void theUnlockMethodShouldNotBeCalled() {
        verify(lock, never()).unlock();
    }

}
