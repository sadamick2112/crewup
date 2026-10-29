package app.crewup.crews.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface MemberRepository extends JpaRepository<Member, UUID> {

    /** The user's Member in a crew that isn't deleted. */
    @Query("""
            select m from Member m join Crew c on c.id = m.crewId
            where m.crewId = :crewId and m.userId = :userId and c.deletedAt is null
            """)
    Optional<Member> findMembership(UUID crewId, UUID userId);
}
