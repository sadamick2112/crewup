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
        return "\"" + version + "\"";
    }

    /**
     * Passes only when {@code ifMatch} names exactly {@code currentVersion}. Anything else, including a weak ETag
     * ({@code W/"3"}), fails: {@code If-Match} uses strong comparison (RFC 9110, 13.1.1).
     */
    public static void requireMatch(String ifMatch, long currentVersion) {
        if (ifMatch == null) {
            throw new PreconditionRequiredException();
        }
        if (!ifMatch.trim().equals(of(currentVersion))) {
            throw new PreconditionFailedException();
        }
    }
}
