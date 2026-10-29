package app.crewup.crews.internal;

import app.crewup.shared.CursorPage;
import app.crewup.shared.Cursors;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CrewService {

    private static final Logger log = LoggerFactory.getLogger(CrewService.class);
    private static final CrewCursor FIRST_PAGE = new CrewCursor("", new UUID(0, 0));

    /** Where a page of My crews ends: the last crew's sort name and id. */
    record CrewCursor(String sortName, UUID id) {}

    private final CrewRepository crews;
    private final MemberRepository members;
    private final Cursors cursors;

    CrewService(CrewRepository crews, MemberRepository members, Cursors cursors) {
        this.crews = crews;
        this.members = members;
        this.cursors = cursors;
    }

    /** Creates the crew and makes the caller its first Member, an Administrator (M1-1, M1-2). */
    @Transactional
    Versioned<CrewDetails> create(UUID userId, CreateCrewRequest request) {
        Instant now = Instant.now();
        Crew crew = crews.saveAndFlush(new Crew(request, userId, now));
        Member creator = members.save(Member.creator(crew.getId(), userId, request.creatorFirstName(), now));
        log.atInfo()
                .addKeyValue("crewId", crew.getId())
                .addKeyValue("memberId", creator.getId())
                .log("Crew created");
        return new Versioned<>(
                CrewDetails.from(crew, new Membership(creator.getId(), creator.getRole())), crew.getVersion());
    }

    /** The user's crews, sorted by name (M1-3). */
    @Transactional(readOnly = true)
    CursorPage<CrewSummary> myCrews(UUID userId, String cursor, int limit) {
        CrewCursor after = cursors.decode(cursor, CrewCursor.class).orElse(FIRST_PAGE);
        List<CrewOfUser> rows = crews.findCrewsOfUser(userId, after.sortName(), after.id(), Limit.of(limit + 1));
        CursorPage<CrewOfUser> page = cursors.page(
                rows, limit, row -> new CrewCursor(row.sortName(), row.crew().getId()));
        return new CursorPage<>(page.items().stream().map(CrewSummary::from).toList(), page.nextCursor());
    }

    /** A crew the caller belongs to; {@code membership} comes from the access check (M1-4). */
    @Transactional(readOnly = true)
    Versioned<CrewDetails> crew(UUID crewId, Membership membership) {
        Crew crew = crews.findById(crewId).orElseThrow(CrewNotFoundException::new);
        return new Versioned<>(CrewDetails.from(crew, membership), crew.getVersion());
    }
}
