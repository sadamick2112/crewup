package app.crewup.shared;

/**
 * Optimistic locking over HTTP (API-7): a read returns the entity's {@code @Version} as an {@code ETag}, and an update
 * must send it back in {@code If-Match}, so a save based on an outdated version is refused instead of silently
 * overwriting someone else's change.
 */
public final class ETags {

    private ETags() {}

    /** The {@code ETag} header value for a version: a strong ETag, quotes included. */
    public static String of(long version) {
        throw new UnsupportedOperationException("not implemented");
    }

    /** Passes only when {@code ifMatch} names exactly {@code currentVersion}. */
    public static void requireMatch(String ifMatch, long currentVersion) {
        throw new UnsupportedOperationException("not implemented");
    }
}
