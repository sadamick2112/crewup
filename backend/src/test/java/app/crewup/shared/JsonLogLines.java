package app.crewup.shared;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.test.system.CapturedOutput;
import tools.jackson.databind.json.JsonMapper;

/** Reads captured ECS JSON log output, one event per line. */
final class JsonLogLines {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private JsonLogLines() {}

    /** The request log lines (those with an {@code http.route} field) in the captured output. */
    static List<Map<String, Object>> requestLines(CapturedOutput output) {
        return output.getOut()
                .lines()
                .filter(line -> line.startsWith("{"))
                .map(JsonLogLines::parse)
                .filter(event -> field(event, "http.route") != null)
                .toList();
    }

    static List<Map<String, Object>> requestLinesForTrace(CapturedOutput output, String traceId) {
        return requestLines(output).stream()
                .filter(event -> traceId.equals(field(event, "traceId")))
                .toList();
    }

    /**
     * Looks up a field by its dotted name, whether the JSON writes it flat ({@code "http.route": …}) or nested
     * ({@code "http": {"route": …}}), so tests don't depend on that formatting detail.
     */
    static Object field(Map<String, Object> event, String dottedName) {
        if (event.containsKey(dottedName)) {
            return event.get(dottedName);
        }
        Object current = event;
        for (String part : dottedName.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(part);
        }
        return current;
    }

    static String text(Map<String, Object> event, String dottedName) {
        return Objects.toString(field(event, dottedName), null);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parse(String line) {
        return JSON.readValue(line, Map.class);
    }
}
