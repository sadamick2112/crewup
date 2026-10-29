package app.crewup.crews.internal;

import app.crewup.identity.Users;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * The caller's Member in each crew a request touches, looked up once per request and shared by the access check and
 * the controller (plan §4.2). Request-scoped: a new instance for every HTTP request.
 */
@Component
@RequestScope
class CallerMemberships {

    private final Users users;
    private final MemberRepository members;
    private final Map<UUID, Optional<Membership>> byCrew = new HashMap<>();

    CallerMemberships(Users users, MemberRepository members) {
        this.users = users;
        this.members = members;
    }

    /** The caller's membership, or {@link CrewNotFoundException} if the crew is unknown, deleted, or not theirs. */
    Membership require(UUID crewId) {
        return byCrew.computeIfAbsent(crewId, this::lookUp).orElseThrow(CrewNotFoundException::new);
    }

    private Optional<Membership> lookUp(UUID crewId) {
        Jwt token = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return members.findMembership(crewId, users.currentUserId(token))
                .map(member -> new Membership(member.getId(), member.getRole()));
    }
}
