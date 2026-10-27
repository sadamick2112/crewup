package app.crewup.shared;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.UUID;

/** Creates entity IDs (JAVA-14). */
public final class Ids {

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final long VERSION_7 = 0x7000L;
    private static final long RAND_A_MASK = 0x0FFFL;
    private static final long RFC_VARIANT = 0x8000_0000_0000_0000L;
    private static final long RAND_B_MASK = 0x3FFF_FFFF_FFFF_FFFFL;

    private Ids() {}

    /** Returns a new UUID v7: time-ordered, so new rows land at the end of primary-key indexes. */
    public static UUID newId() {
        return newId(Clock.systemUTC());
    }

    /**
     * Layout from RFC 9562 §5.7: a 48-bit Unix-millisecond timestamp, the version (4 bits), 12 random bits, the
     * variant (2 bits), and 62 random bits.
     */
    static UUID newId(Clock clock) {
        long mostSignificantBits = (clock.millis() << 16) | VERSION_7 | (RANDOM.nextLong() & RAND_A_MASK);
        long leastSignificantBits = RFC_VARIANT | (RANDOM.nextLong() & RAND_B_MASK);
        return new UUID(mostSignificantBits, leastSignificantBits);
    }
}
