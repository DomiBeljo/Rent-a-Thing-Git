package org.example.rentathingproba;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularStructureTest {

    @Test
    void verifyModularStructure() {
        ApplicationModules.of(RentAThingProbaApplication.class).verify();
    }
}
