package app.crewup;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@IntegrationTest
class SwaggerUiTest {

    private final MockMvcTester mvc;

    @Autowired
    SwaggerUiTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    void M0_9_swaggerUiShortLinkRedirectsWithoutToken() {
        var response = mvc.get().uri("/swagger-ui.html").exchange();

        assertThat(response).hasStatus3xxRedirection();
        assertThat(response.getResponse().getRedirectedUrl()).startsWith("/swagger-ui/index.html");
    }
}
