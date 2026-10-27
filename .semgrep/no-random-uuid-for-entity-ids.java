// Fixture for no-random-uuid-for-entity-ids (JAVA-14, M0-16c): `semgrep --test .semgrep` checks every annotated line.
package fixtures;

import app.crewup.shared.Ids;
import java.util.UUID;

class RandomUuid {

    void randomIds() {
        // ruleid: no-random-uuid-for-entity-ids
        UUID id = UUID.randomUUID();
        // ruleid: no-random-uuid-for-entity-ids
        String name = "user-" + java.util.UUID.randomUUID();
    }

    void otherIds(String text) {
        // ok: no-random-uuid-for-entity-ids
        UUID id = Ids.newId();
        // ok: no-random-uuid-for-entity-ids
        UUID parsed = UUID.fromString(text);
    }
}
