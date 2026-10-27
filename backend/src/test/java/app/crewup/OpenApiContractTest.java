package app.crewup;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Keeps the committed API contract ({@code api/openapi.json}) in step with the code (M0-9). The frontend's client is
 * generated from that file, so an API change must update it in the same commit. To regenerate it:
 *
 * <pre>./mvnw test -Dtest=OpenApiContractTest -Dopenapi.update=true</pre>
 */
@IntegrationTest
class OpenApiContractTest {

    // Tests run from backend/, and the contract lives at the repository root.
    private static final Path CONTRACT = Path.of("..", "api", "openapi.json");
    private static final JsonMapper JSON =
            JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

    private final MockMvcTester mvc;

    @Autowired
    OpenApiContractTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    void M0_9_openApiContractIsUpToDate() throws IOException {
        JsonNode generated = JSON.readTree(
                mvc.get().uri("/v3/api-docs").exchange().getResponse().getContentAsString());

        if (Boolean.getBoolean("openapi.update")) {
            Files.createDirectories(CONTRACT.getParent());
            Files.writeString(CONTRACT, JSON.writeValueAsString(generated) + "\n");
        }

        assertThat(CONTRACT)
                .as("Contract file missing; regenerate it (see class comment)")
                .exists();
        assertThat(generated)
                .as("The API changed but %s didn't; regenerate it (see class comment) and commit both", CONTRACT)
                .isEqualTo(JSON.readTree(Files.readString(CONTRACT)));
    }
}
