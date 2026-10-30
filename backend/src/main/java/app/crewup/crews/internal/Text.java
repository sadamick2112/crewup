package app.crewup.crews.internal;

/** Request text is stored trimmed, and an optional field left blank is stored as null (spec §3, field rules). */
final class Text {

    private Text() {}

    static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
