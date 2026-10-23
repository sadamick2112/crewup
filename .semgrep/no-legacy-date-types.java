// Fixture for no-legacy-date-types (JAVA-9, M0-16c): `semgrep --test .semgrep` checks every annotated line.
package fixtures;

// ruleid: no-legacy-date-types
import java.util.Date;
// ruleid: no-legacy-date-types
import java.util.Calendar;
// ruleid: no-legacy-date-types
import java.sql.Timestamp;
// ok: no-legacy-date-types
import java.time.Instant;
// ok: no-legacy-date-types
import java.time.ZonedDateTime;

class LegacyDateTypes {

    // ruleid: no-legacy-date-types
    private java.util.Date fullyQualifiedField;

    // ok: no-legacy-date-types
    private Instant createdAt;

    // ok: no-legacy-date-types
    private ZonedDateTime startsAt;

    void fullyQualifiedUse() {
        // ruleid: no-legacy-date-types
        var now = java.util.Calendar.getInstance();
        // ruleid: no-legacy-date-types
        var stamp = new java.sql.Timestamp(0L);
        // ok: no-legacy-date-types
        var instant = Instant.now();
    }
}
