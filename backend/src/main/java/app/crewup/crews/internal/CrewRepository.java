package app.crewup.crews.internal;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface CrewRepository extends JpaRepository<Crew, UUID> {

    /**
     * One page of the user's crews, after the given sort key (keyset pagination, API-7). The first page passes an
     * empty name and the nil UUID, which every crew sorts after, so there is one query instead of two.
     */
    @Query("""
            select new app.crewup.crews.internal.CrewOfUser(c, m.role, lower(c.name))
            from Crew c join Member m on m.crewId = c.id
            where m.userId = :userId and c.deletedAt is null
              and (lower(c.name) > :afterName or (lower(c.name) = :afterName and c.id > :afterId))
            order by lower(c.name), c.id
            """)
    List<CrewOfUser> findCrewsOfUser(UUID userId, String afterName, UUID afterId, Limit limit);
}
