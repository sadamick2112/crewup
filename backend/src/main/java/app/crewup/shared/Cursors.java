package app.crewup.shared;

import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
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
        if (cursor == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(json.readValue(Base64.getUrlDecoder().decode(cursor), keyType));
        } catch (IllegalArgumentException | JacksonException e) {
            // Not Base64, or not this list's key: a cursor the client made up or copied from another list.
            throw new InvalidRequestException("cursor", "Not a cursor from this list");
        }
    }

    /**
     * Builds a page from rows fetched with {@code limit + 1}: the extra row only shows that another page exists, and
     * the cursor is the key of the last row kept.
     */
    public <T> CursorPage<T> page(List<T> rows, int limit, Function<T, ?> keyOf) {
        if (rows.size() <= limit) {
            return new CursorPage<>(rows, null);
        }
        List<T> items = List.copyOf(rows.subList(0, limit));
        return new CursorPage<>(items, encode(keyOf.apply(items.getLast())));
    }

    private String encode(Object key) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.writeValueAsBytes(key));
    }
}
