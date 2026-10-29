package app.crewup.identity.internal;

import app.crewup.identity.Users;
import app.crewup.shared.Ids;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Finds the User behind an access token, creating it on the first call (M0-6). */
@Service
class UserProvisioningService implements Users {

    private static final Logger log = LoggerFactory.getLogger(UserProvisioningService.class);

    private final UserRepository users;

    UserProvisioningService(UserRepository users) {
        this.users = users;
    }

    @Transactional
    User currentUser(Jwt jwt) {
        UserClaims claims = UserClaims.from(jwt);
        User user = users.findBySubject(claims.subject()).orElseGet(() -> provision(claims));
        user.refresh(claims, Instant.now());
        return user;
    }

    // Transactional itself: calling currentUser through `this` bypasses the proxy, so its annotation wouldn't apply.
    @Override
    @Transactional
    public UUID currentUserId(Jwt token) {
        return currentUser(token).getId();
    }

    private User provision(UserClaims claims) {
        UUID id = Ids.newId();
        int inserted = users.insertIfAbsent(id, claims.subject(), claims.email(), claims.displayName(), Instant.now());
        if (inserted == 1) {
            log.atInfo().addKeyValue("userId", id).log("User provisioned");
        }
        // If a simultaneous call inserted first, its row is the one to use.
        return users.findBySubject(claims.subject()).orElseThrow();
    }
}
