package com.grupob.bancobanquito;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTest {

    @Test
    void verifiesModuleBoundaries() {
        ApplicationModules.of(BancoBanQuitoApplication.class).verify();
    }
}
