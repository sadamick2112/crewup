package app.crewup.shared;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Cursor pagination (API-7). A cursor is the last returned item's sort key, as Base64url-encoded JSON, so a query can
 * continue with "rows after this key" (keyset pagination). Clients treat it as opaque.
 */
@Component
public class Cursors {

    public static final int DEFAULT_LIMIT = 50;
    public static final int MAX_LIMIT = 100;

    private final JsonMapper json;

    Cursors(JsonMapper json) {
        this.json = json;
    }

    /** The sort key a cursor holds; empty when there is no cursor (the first page). */
    public <K> Optional<K> decode(String cursor, Class<K> keyType) {
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * Builds a page from rows fetched with {@code limit + 1}: the extra row only shows that another page exists, and
     * the cursor is the key of the last row kept.
     */
    public <T> CursorPage<T> page(List<T> rows, int limit, Function<T, ?> keyOf) {
        throw new UnsupportedOperationException("not implemented");
    }
}
