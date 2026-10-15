package app.crewup.identity.internal;

import java.util.UUID;

/** The signed-in user's profile, as returned by {@code GET /api/me}. */
record UserProfile(UUID id, String displayName, String email) {}
