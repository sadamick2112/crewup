package app.crewup.identity.internal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A person who signs in to CrewUp, linked to the identity provider by the token's subject. Rows are created by
 * {@link UserRepository#insertIfAbsent}, so this entity has no public constructor.
 */
@Entity
@Table(name = "users")
class User {

    @Id
    private UUID id;

    private String subject;
    private String email;
    private String displayName;
    private Instant createdAt;
    private Instant updatedAt;

    protected User() {
        // for JPA
    }

    UUID getId() {
        return id;
    }

    String getEmail() {
        return email;
    }

    String getDisplayName() {
        return displayName;
    }

    /** Copies the token's current email and display name, so changes made at the identity provider show up here. */
    void refresh(UserClaims claims, Instant now) {
        if (Objects.equals(email, claims.email()) && displayName.equals(claims.displayName())) {
            return;
        }
        email = claims.email();
        displayName = claims.displayName();
        updatedAt = now;
    }
}
