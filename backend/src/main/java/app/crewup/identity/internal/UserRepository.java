package app.crewup.identity.internal;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findBySubject(String subject);

    /**
     * Inserts a user unless one with this subject already exists, and returns the number of rows inserted (1 or 0).
     * {@code ON CONFLICT DO NOTHING} lets simultaneous first calls race safely: the losing insert waits for the
     * winner to commit and then does nothing, instead of failing the request (a failed insert would abort the whole
     * PostgreSQL transaction).
     */
    @Modifying
    @Query(value = """
                    INSERT INTO users (id, subject, email, display_name, created_at, updated_at)
                    VALUES (:id, :subject, :email, :displayName, :now, :now)
                    ON CONFLICT (subject) DO NOTHING
                    """, nativeQuery = true)
    int insertIfAbsent(UUID id, String subject, String email, String displayName, Instant now);
}
