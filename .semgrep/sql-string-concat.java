// Fixture for sql-string-concat (JAVA-13, M0-16a): `semgrep --test .semgrep` checks every annotated line.
package fixtures;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;

class SqlStringConcat {

    private static final String SELECT_EVENTS = "select e from Event e";

    private final EntityManager entityManager;
    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate namedJdbc;
    private final JdbcClient jdbcClient;

    SqlStringConcat(
            EntityManager entityManager, JdbcTemplate jdbc, NamedParameterJdbcTemplate namedJdbc, JdbcClient jdbcClient) {
        this.entityManager = entityManager;
        this.jdbc = jdbc;
        this.namedJdbc = namedJdbc;
        this.jdbcClient = jdbcClient;
    }

    void concatenated(String name, UUID id) {
        // ruleid: sql-string-concat
        entityManager.createQuery("select e from Event e where e.name = '" + name + "'");
        // ruleid: sql-string-concat
        entityManager.createNativeQuery("select * from events where id = " + id);
        // ruleid: sql-string-concat
        jdbc.queryForList("select * from events where name = '" + name + "'");
        // ruleid: sql-string-concat
        namedJdbc.queryForList("select * from events where name = '" + name + "'", java.util.Map.of());
        // ruleid: sql-string-concat
        jdbcClient.sql("select * from events where id = " + id);
    }

    void formatted(String name) {
        // ruleid: sql-string-concat
        entityManager.createQuery(String.format("select e from Event e where e.name = '%s'", name));
        // ruleid: sql-string-concat
        jdbc.update("delete from events where name = '%s'".formatted(name));
    }

    void builtInAVariable(UUID id) {
        String sql = "delete from events where id = " + id;
        // ruleid: sql-string-concat
        jdbc.update(sql);
    }

    void boundParameters(String name, UUID id) {
        // ok: sql-string-concat
        entityManager.createQuery("select e from Event e where e.name = :name").setParameter("name", name);
        // ok: sql-string-concat
        entityManager.createNativeQuery("select * from events where id = ?1").setParameter(1, id);
        // ok: sql-string-concat
        jdbc.queryForList("select * from events where name = ?", name);
        // ok: sql-string-concat
        jdbcClient.sql("select * from events where id = :id").param("id", id);
    }

    void constantsOnly() {
        // ok: sql-string-concat
        entityManager.createQuery("select e from Event e " + "where e.name = :name");
        // ok: sql-string-concat
        entityManager.createQuery(SELECT_EVENTS + " where e.name = :name");
    }

    interface EventRepository {
        // ok: sql-string-concat
        @Query("select e from Event e where e.name = :name")
        List<Object> findByName(String name);
    }
}
