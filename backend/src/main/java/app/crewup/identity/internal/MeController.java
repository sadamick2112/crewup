package app.crewup.identity.internal;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class MeController {

    private final UserProvisioningService provisioning;

    MeController(UserProvisioningService provisioning) {
        this.provisioning = provisioning;
    }

    @GetMapping("/api/me")
    UserProfile me(@AuthenticationPrincipal Jwt jwt) {
        return UserProfile.from(provisioning.currentUser(jwt));
    }
}
