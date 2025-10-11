package guru.nicks.cucumber;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class RedisCacheSteps {

    private final Map<Integer, UUID> values = new HashMap<>();

    // DI
    private final RedisCacheTestComponent redisCacheTestComponent;

    @When("a non-cacheable method is called to generate UUID{int}")
    public void generateUncacheableUUID(int i) {
        values.put(i, redisCacheTestComponent.generateUncacheableUUID());
    }

    @When("a cacheable method is called to generate UUID{int}")
    public void generateCacheableUUID(int i) {
        values.put(i, redisCacheTestComponent.generateCacheableUUID());
    }

    @Then("UUID{int} is equal to UUID{int}")
    public void uuid1EqualsUuid2(int i, int j) {
        assertThat(values).containsEntry(i, values.get(j));
    }

    @Then("UUID{int} is not equal to UUID{int}")
    public void uuid1DoesNotEqualUuid2(int i, int j) {
        assertThat(values).doesNotContainEntry(i, values.get(j));
    }

}
