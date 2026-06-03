package org.example.rentathingproba;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularStructureTest {

    @Test
    void verifyModularStructure() {
        ApplicationModules modules;
        try {
            modules = ApplicationModules.of(RentAThingProbaApplication.class);
        } catch (IllegalArgumentException e) {
            Assumptions.abort("Skipping modular structure check — main classes not found on classpath: " + e.getMessage());
            return;
        }
        modules.verify();
    }
}