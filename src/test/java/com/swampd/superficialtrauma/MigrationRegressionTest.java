package com.swampd.superficialtrauma;

import org.junit.jupiter.api.Test;

/** Keep the existing balance and state-machine suite running in NeoForge's bootstrapped test environment. */
public class MigrationRegressionTest {
    @Test
    void medicalStateMachinesAndPersistence() {
        com.swampd.superficialtrauma.common.body.BodyStateRoundTripTest.main(new String[0]);
    }

    @Test
    void visualThresholdsAndEasing() {
        com.swampd.superficialtrauma.client.VitalSignsVisualStateTest.main(new String[0]);
    }
}
