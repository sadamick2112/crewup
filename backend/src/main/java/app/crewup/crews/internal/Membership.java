package app.crewup.crews.internal;

import java.util.UUID;

/** The caller's place in one crew: their Member id and role. */
record Membership(UUID memberId, MemberRole role) {}
