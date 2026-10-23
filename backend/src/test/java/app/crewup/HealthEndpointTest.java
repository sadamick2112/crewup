package app.crewup;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@IntegrationTest
class HealthEndpointTest {

    private final MockMvcTester mvc;

    @Autowired
    HealthEndpointTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    void M0_3_healthIsPublicAndReportsDatabase() {
        var response = mvc.get().uri("/actuator/health").exchange();

        assertThat(response).hasStatus(HttpStatus.OK);
        assertThat(response).bodyJson().extractingPath("$.status").isEqualTo("UP");
        assertThat(response).bodyJson().extractingPath("$.components.db.status").isEqualTo("UP");
        assertThat(response).bodyJson().doesNotHavePath("$.components.db.details");
    }
}
