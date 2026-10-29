package app.crewup.crews.internal;

import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * The checks {@code @PreAuthorize} calls on crew endpoints (M1-16), as {@code @crewAccess}. A non-member gets
 * {@link CrewNotFoundException} (404) rather than {@code false} (403), so a stranger can't tell the crew exists.
 */
@Component("crewAccess")
class CrewAccess {

    private final CallerMemberships memberships;

    CrewAccess(CallerMemberships memberships) {
        this.memberships = memberships;
    }

    public boolean isMember(UUID crewId) {
        memberships.require(crewId);
        return true;
    }
}
