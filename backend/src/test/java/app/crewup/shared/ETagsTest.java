package app.crewup.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ETagsTest {

    @Test
    void M1_5_etagIsTheQuotedVersion() {
        assertThat(ETags.of(3)).isEqualTo("\"3\"");
    }

    @Test
    void M1_5_matchingIfMatchPasses() {
        assertThatCode(() -> ETags.requireMatch("\"3\"", 3)).doesNotThrowAnyException();
    }

    @Test
    void M1_5_missingIfMatchIsRequired() {
        assertThatThrownBy(() -> ETags.requireMatch(null, 3)).isInstanceOf(PreconditionRequiredException.class);
    }

    // A weak ETag never matches: If-Match uses strong comparison (RFC 9110, 13.1.1).
    @ParameterizedTest
    @ValueSource(strings = {"\"2\"", "\"4\"", "3", "W/\"3\"", "garbage", ""})
    void M1_5_anyOtherIfMatchFails(String ifMatch) {
        assertThatThrownBy(() -> ETags.requireMatch(ifMatch, 3)).isInstanceOf(PreconditionFailedException.class);
    }
}
