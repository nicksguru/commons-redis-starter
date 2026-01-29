package guru.nicks.commons.cucumber;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class RedisCacheSteps {

    // for possible parallel scenarios
    private final Map<Integer, String> values = new ConcurrentHashMap<>();

    // DI
    private final RedisCacheTestComponent redisCacheTestComponent;

    @When("a non-cacheable method is called to generate string{int}")
    public void generateUncacheableString(int i) {
        values.put(i, redisCacheTestComponent.generateUncacheableString());
    }

    @When("a cacheable method is called to generate string{int}")
    public void generateCacheableString(int i) {
        values.put(i, redisCacheTestComponent.generateCacheableString());
    }

    @Then("string{int} is equal to string{int}")
    public void string1EqualsToString2(int i, int j) {
        assertThat(values).containsEntry(i, values.get(j));
    }

    @Then("string{int} is not equal to string{int}")
    public void string1DoesNotEqualToString2(int i, int j) {
        assertThat(values).doesNotContainEntry(i, values.get(j));
    }

}
