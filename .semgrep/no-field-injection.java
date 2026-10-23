// Fixture for no-field-injection (JAVA-3, M0-16c): `semgrep --test .semgrep` checks every annotated line.
package fixtures;

import jakarta.inject.Inject;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Autowired;

class FieldInjection {

    // ruleid: no-field-injection
    @Autowired
    private Clock clock;

    // ruleid: no-field-injection
    @Inject
    private Clock injectedClock;

    private Clock setterClock;

    // ruleid: no-field-injection
    @Autowired
    void setClock(Clock clock) {
        this.setterClock = clock;
    }
}

class ConstructorInjection {

    // ok: no-field-injection
    private final Clock clock;

    // ok: no-field-injection
    ConstructorInjection(Clock clock) {
        this.clock = clock;
    }
}

class AnnotatedConstructorInjection {

    private final Clock clock;

    // ok: no-field-injection
    @Autowired
    AnnotatedConstructorInjection(Clock clock) {
        this.clock = clock;
    }
}
