package app.crewup.crews.internal;

import app.crewup.shared.Ids;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;

/** A crew of friends: the top-level tenant that all other data belongs to. */
@Entity
@Table(name = "crews")
class Crew {

    @Id
    private UUID id;

    private String name;
    private YearMonth establishedMonth;
    private String motto;
    private String description;

    @Enumerated(EnumType.STRING)
    private CrewIcon iconKey;

    private String timeZone;

    // A wrapper type, so Spring Data sees a new entity (null version) and persists it instead of merging.
    @Version
    private Long version;

    private Instant createdAt;
    private UUID createdBy;
    private Instant updatedAt;
    private UUID updatedBy;
    private Instant deletedAt;

    protected Crew() {
        // for JPA
    }

    Crew(CreateCrewRequest request, UUID userId, Instant now) {
        this.id = Ids.newId();
        this.name = request.name().trim();
        this.establishedMonth = request.establishedMonth();
        this.motto = Text.trimToNull(request.motto());
        this.description = Text.trimToNull(request.description());
        this.iconKey = request.iconKey();
        this.timeZone = request.timeZone();
        this.createdAt = now;
        this.createdBy = userId;
        this.updatedAt = now;
        this.updatedBy = userId;
    }

    void update(UpdateCrewRequest request, UUID userId, Instant now) {
        this.name = request.name().trim();
        this.establishedMonth = request.establishedMonth();
        this.motto = Text.trimToNull(request.motto());
        this.description = Text.trimToNull(request.description());
        this.iconKey = request.iconKey();
        this.timeZone = request.timeZone();
        this.updatedAt = now;
        this.updatedBy = userId;
    }

    /** Hides the crew from everyone; {@code CrewPurgeJob} erases it 30 days later (M1-6, M1-7). */
    void delete(UUID userId, Instant now) {
        this.deletedAt = now;
        this.updatedAt = now;
        this.updatedBy = userId;
    }

    UUID getId() {
        return id;
    }

    String getName() {
        return name;
    }

    YearMonth getEstablishedMonth() {
        return establishedMonth;
    }

    String getMotto() {
        return motto;
    }

    String getDescription() {
        return description;
    }

    CrewIcon getIconKey() {
        return iconKey;
    }

    String getTimeZone() {
        return timeZone;
    }

    long getVersion() {
        return version;
    }
}
