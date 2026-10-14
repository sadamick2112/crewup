package app.crewup;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

    @Test
    void M0_8_moduleBoundariesAreRespected() {
        ApplicationModules.of(CrewUpApplication.class).verify();
    }
}
