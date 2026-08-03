package guru.nicks.commons.cucumber;

import guru.nicks.commons.auth.domain.BlockedTokenHash;
import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.redis.impl.BlockedJwtServiceImpl;
import guru.nicks.commons.redis.repository.BlockedTokenRepository;
import guru.nicks.commons.service.BlockedJwtService;
import guru.nicks.commons.utils.auth.AuthUtils;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RequiredArgsConstructor
public class BlockedAccessTokenServiceSteps {

    // DI
    private final TextWorld textWorld;

    @Mock
    private BlockedTokenRepository blockedTokenRepository;
    @Captor
    private ArgumentCaptor<BlockedTokenHash> blockedTokenHashCaptor;
    private AutoCloseable closeableMocks;

    private BlockedJwtService blockedJwtService;
    private boolean isBlockedResult;
    private Jwt ifBelongsToUserResult;

    private Jwt accessToken;
    private String userId;

    @Before
    public void beforeEachScenario() {
        closeableMocks = MockitoAnnotations.openMocks(this);
        blockedJwtService = new BlockedJwtServiceImpl(blockedTokenRepository);
    }

    @After
    public void afterEachScenario() throws Exception {
        closeableMocks.close();
    }

    @Given("the access token is {booleanValue}")
    public void theAccessTokenIs(boolean isBlocked) {
        accessToken = Jwt.withTokenValue("some-token")
                .header("alg", "none")
                .claim("sub", "test-user")
                .build();
        String checksum = AuthUtils.calculateAccessTokenChecksum(accessToken.getTokenValue());

        when(blockedTokenRepository.existsById(checksum))
                .thenReturn(isBlocked);
    }

    @Given("a valid access token with an expiration of {long} seconds")
    public void aValidAccessTokenWithAnExpirationOfSeconds(long seconds) {
        aValidAccessTokenWithSubjectAndAnExpirationOfSeconds("test-user", seconds);
    }

    @When("'isBlocked' is called for the access token")
    public void isBlockedIsCalledForTheAccessToken() {
        textWorld.setLastException(catchThrowable(() ->
                isBlockedResult = blockedJwtService.isJwtBlocked(accessToken)));
    }

    @When("'blockJwt' is called for the access token")
    public void blockJwtIsCalledForTheAccessToken() {
        textWorld.setLastException(catchThrowable(() ->
                blockedJwtService.blockJwt(accessToken)));
    }

    @Given("a valid access token with subject {string} and an expiration of {long} seconds")
    public void aValidAccessTokenWithSubjectAndAnExpirationOfSeconds(String subject, long seconds) {
        accessToken = Jwt.withTokenValue("token-" + subject)
                .header("alg", "HS256")
                .claim("sub", subject)
                .expiresAt(Instant.now().plusSeconds(seconds))
                .build();
    }

    @Given("the user ID is {string}")
    public void theUserIdIs(String userId) {
        this.userId = userId;
    }

    @When("'ifBelongsToUser' is called for the access token")
    public void ifBelongsToUserIsCalledForTheAccessToken() {
        textWorld.setLastException(catchThrowable(() ->
                ifBelongsToUserResult = blockedJwtService.ifBelongsToUser(accessToken, userId, Function.identity())));
    }

    @Then("the mapper should have been called with the access token")
    public void theMapperShouldHaveBeenCalledWithTheAccessToken() {
        assertThat(ifBelongsToUserResult)
                .as("mapper result")
                .isEqualTo(accessToken);
    }

    @Then("the result should be {booleanValue}")
    public void theResultShouldBe(boolean expectedResult) {
        assertThat(isBlockedResult)
                .as("isBlocked result")
                .isEqualTo(expectedResult);
    }

    @Then("the access token should be blocked with a TTL greater than {long} seconds")
    public void theAccessTokenShouldBeBlockedWithATTLGreaterThanSeconds(long ttl) {
        verify(blockedTokenRepository).save(blockedTokenHashCaptor.capture());
        var savedHash = blockedTokenHashCaptor.getValue();

        assertThat(savedHash.getTokenChecksum())
                .as("access token checksum")
                .isEqualTo(AuthUtils.calculateAccessTokenChecksum(accessToken.getTokenValue()));

        assertThat(savedHash.getTimeToLiveSec())
                .as("time to live")
                .isGreaterThan(ttl);
    }

}
