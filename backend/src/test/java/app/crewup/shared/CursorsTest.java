package app.crewup.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class CursorsTest {

    record SortKey(String name, long id) {}

    private final Cursors cursors = new Cursors(JsonMapper.builder().build());

    @Test
    void M1_11_noCursorMeansTheFirstPage() {
        assertThat(cursors.decode(null, SortKey.class)).isEmpty();
    }

    @Test
    void M1_11_nextCursorHoldsTheLastKeptRowsKey() {
        var page = cursors.page(List.of("a", "b", "c"), 2, row -> new SortKey(row, row.length()));

        assertThat(page.items()).containsExactly("a", "b");
        assertThat(cursors.decode(page.nextCursor(), SortKey.class)).contains(new SortKey("b", 1));
    }

    @Test
    void M1_11_lastPageHasNoNextCursor() {
        var page = cursors.page(List.of("a", "b"), 2, row -> new SortKey(row, 1));

        assertThat(page.items()).containsExactly("a", "b");
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    void M1_11_tamperedCursorIsRejectedAsInvalidField() {
        assertThatThrownBy(() -> cursors.decode("not-a-cursor", SortKey.class))
                .isInstanceOfSatisfying(
                        InvalidRequestException.class,
                        e -> assertThat(e.invalidField().field()).isEqualTo("cursor"));
    }
}
