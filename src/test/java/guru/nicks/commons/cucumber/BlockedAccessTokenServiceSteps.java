package guru.nicks.commons.cucumber;

import guru.nicks.commons.auth.domain.BlockedTokenHash;
import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.redis.impl.BlockedJwtServiceImpl;
import guru.nicks.commons.redis.repository.BlockedTokenRepository;
import guru.nicks.commons.service.BlockedJwtService;
import guru.nicks.commons.utils.auth.AuthUtils;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
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

import java.time.Instant;
import java.util.Date;

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
    private String accessToken;

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
        accessToken = "some-token";
        String checksum = AuthUtils.calculateAccessTokenChecksum(accessToken);

        when(blockedTokenRepository.existsById(checksum))
                .thenReturn(isBlocked);
    }

    @Given("a valid access token with an expiration of {long} seconds")
    public void aValidAccessTokenWithAnExpirationOfSeconds(long seconds) throws JOSEException {
        var claims = new JWTClaimsSet.Builder()
                .subject("test-user")
                .issuer("https://example.com")
                .expirationTime(Date.from(Instant.now().plusSeconds(seconds)))
                .build();
        var signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);

        // WARNING: this is simpler then using a key pair, but allowed for testing only!
        // The secret length must be at least 256 bits.
        var signer = new MACSigner("test-256-bit-secret-test-256-bit-secret-test-256-bit-secret");
        signedJWT.sign(signer);
        accessToken = signedJWT.serialize();
    }

    @Given("the following access token value")
    public void theFollowingAccessTokenValue(String accessToken) {
        this.accessToken = accessToken;
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
                .isEqualTo(AuthUtils.calculateAccessTokenChecksum(accessToken));

        assertThat(savedHash.getTimeToLiveSec())
                .as("time to live")
                .isGreaterThan(ttl);
    }

}
