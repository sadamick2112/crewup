package app.crewup.shared;

import java.time.Clock;
import java.util.UUID;

/** Creates entity IDs (JAVA-14). */
public final class Ids {

    private Ids() {}

    /** Returns a new UUID v7: time-ordered, so new rows land at the end of primary-key indexes. */
    public static UUID newId() {
        throw new UnsupportedOperationException("not implemented");
    }

    static UUID newId(Clock clock) {
        throw new UnsupportedOperationException("not implemented");
    }
}
