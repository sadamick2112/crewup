package app.crewup.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdsTest {

    private static final Instant NOW = Instant.parse("2026-10-14T18:30:00.123Z");

    @Test
    void JAVA14_idIsVersion7WithRfcVariant() {
        var id = Ids.newId();

        assertThat(id.version()).isEqualTo(7);
        assertThat(id.variant()).isEqualTo(2);
    }

    @Test
    void JAVA14_embeddedTimestampMatchesClock() {
        var id = Ids.newId(Clock.fixed(NOW, ZoneOffset.UTC));

        long embeddedMillis = id.getMostSignificantBits() >>> 16;
        assertThat(embeddedMillis).isEqualTo(NOW.toEpochMilli());
    }

    @Test
    void JAVA14_idsFromLaterMillisecondsSortAfterEarlierOnes() {
        var earlierClock = Clock.fixed(NOW, ZoneOffset.UTC);
        var laterClock = Clock.fixed(NOW.plus(Duration.ofMillis(1)), ZoneOffset.UTC);

        for (int i = 0; i < 100; i++) {
            UUID earlier = Ids.newId(earlierClock);
            UUID later = Ids.newId(laterClock);

            // String order is byte order, the order PostgreSQL uses for uuid columns.
            assertThat(later.toString()).isGreaterThan(earlier.toString());
        }
    }

    @Test
    void JAVA14_noDuplicatesInLargeBatch() {
        // All in the same millisecond: uniqueness then rests on the random bits alone.
        var clock = Clock.fixed(NOW, ZoneOffset.UTC);
        var ids = new HashSet<UUID>();

        for (int i = 0; i < 100_000; i++) {
            ids.add(Ids.newId(clock));
        }

        assertThat(ids).hasSize(100_000);
    }
}
