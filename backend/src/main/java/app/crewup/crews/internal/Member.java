package app.crewup.crews.internal;

import app.crewup.shared.Ids;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/** A person's profile in one crew, linked to a User unless they're a historical member. Holds the role (ROL-1). */
@Entity
@Table(name = "members")
class Member {

    @Id
    private UUID id;

    private UUID crewId;
    private UUID userId;

    @Enumerated(EnumType.STRING)
    private MemberRole role;

    private String firstName;

    @Enumerated(EnumType.STRING)
    private MemberStatus status;

    @Version
    private Long version;

    private Instant createdAt;
    private UUID createdBy;
    private Instant updatedAt;
    private UUID updatedBy;

    protected Member() {
        // for JPA
    }

    private Member(UUID crewId, UUID userId, MemberRole role, String firstName, UUID authorId, Instant now) {
        this.id = Ids.newId();
        this.crewId = crewId;
        this.userId = userId;
        this.role = role;
        this.firstName = firstName.trim();
        this.status = MemberStatus.ACTIVE;
        this.createdAt = now;
        this.createdBy = authorId;
        this.updatedAt = now;
        this.updatedBy = authorId;
    }

    /** The crew's creator: its first Member, an active Administrator linked to their User (M1-2). */
    static Member creator(UUID crewId, UUID userId, String firstName, Instant now) {
        return new Member(crewId, userId, MemberRole.ADMIN, firstName, userId, now);
    }

    UUID getId() {
        return id;
    }

    MemberRole getRole() {
        return role;
    }
}
