package com.swampd.superficialtrauma.client;

public final class VitalSignsVisualStateTest {
    public static void main(String[] args) {
        verifyThresholdsAndLinearStrength();
        verifySuddenChangesEaseInAndOut();
        verifySamplingDoesNotAdvanceTime();
        verifyClearResetsEveryChannel();
        System.out.println("Superficial Trauma vital-sign visuals self-test passed.");
    }

    private static void verifyThresholdsAndLinearStrength() {
        for (float distress : new float[] {-10.0F, 0.0F, 3.0F}) {
            require(!settled(0, distress).visible(), "Distress <= 3 must have no visual effect");
        }
        var threshold = settled(0, 3.1F);
        require(threshold.respiratoryEdge() > 0.0F, "Distress just above 3 must start the border");
        close(0.0F, threshold.respiratoryDim(), "Low distress must leave the center clear");
        close(0.5F, settled(0, 11.5F).respiratoryEdge(), "Border strength must be linear from 3 to 20");
        close(0.0F, settled(0, 15.0F).respiratoryDim(), "Exactly 15 must not darken the whole screen");
        require(settled(0, 15.1F).respiratoryDim() > 0.0F, "Distress above 15 must begin full-screen dimming");
        close(0.5F, settled(0, 17.5F).respiratoryDim(), "Dimming must grow linearly from 15 to 20");
        close(1.0F, settled(0, 20.0F).respiratoryEdge(), "Twenty must reach maximum border strength");
        close(1.0F, settled(0, 20.0F).respiratoryDim(), "Twenty must reach maximum dimming");

        require(!settled(-3, 0).visible(), "Bradycardia alone must not cause tachycardia redness");
        close(1.0F / 3.0F, settled(1, 0).redness(), "Tachycardia one must have light redness");
        close(2.0F / 3.0F, settled(2, 0).redness(), "Tachycardia two must have stronger redness");
        close(0.0F, settled(2, 0).veins(), "The first two levels must not show blood vessels");
        close(1.0F, settled(3, 0).veins(), "Tachycardia three must show fine blood vessels");
    }

    private static void verifySuddenChangesEaseInAndOut() {
        VitalSignsVisualState state = new VitalSignsVisualState();
        state.tick(3, 20);
        var first = state.frame(1);
        require(first.redness() > 0 && first.redness() < 0.1F, "Instant level gain must fade in");
        require(first.veins() > 0 && first.veins() < 0.1F, "Instant level three must fade vessels in");
        require(first.respiratoryDim() > 0 && first.respiratoryDim() < 0.1F, "Instant distress must fade dimming in");
        float previous = first.respiratoryEdge();
        for (int tick = 0; tick < 100; tick++) {
            state.tick(3, 20);
            float current = state.frame(1).respiratoryEdge();
            require(current >= previous && current <= 1, "Fade-in must not flicker or overshoot");
            previous = current;
        }
        state.tick(0, -10);
        var firstOut = state.frame(1);
        require(firstOut.respiratoryEdge() > 0.9F && firstOut.respiratoryEdge() < 1,
                "Instant drug removal must fade the border out");
        require(firstOut.redness() > 0.9F && firstOut.veins() > 0.9F
                        && firstOut.respiratoryDim() > 0.9F,
                "All independent channels must fade out after instant removal");
        previous = firstOut.respiratoryEdge();
        for (int tick = 0; tick < 100; tick++) {
            state.tick(0, -10);
            float current = state.frame(1).respiratoryEdge();
            require(current <= previous && current >= 0, "Fade-out must not flicker or overshoot");
            previous = current;
        }
        require(!state.frame(1).visible(), "No tinted residue may remain after fade-out");
    }

    private static void verifySamplingDoesNotAdvanceTime() {
        VitalSignsVisualState state = new VitalSignsVisualState();
        state.tick(3, 20);
        close(0, state.frame(0).redness(), "A new tick must interpolate from the previous frame");
        close(state.frame(1).redness() / 2, state.frame(0.5F).redness(),
                "Partial ticks must interpolate smoothly");
        var expected = state.frame(0.5F);
        for (int frame = 0; frame < 240; frame++) {
            require(expected.equals(state.frame(0.5F)), "Higher rendering FPS must not accelerate effects");
        }
    }

    private static void verifyClearResetsEveryChannel() {
        VitalSignsVisualState state = new VitalSignsVisualState();
        for (int tick = 0; tick < 40; tick++) {
            state.tick(3, 20);
        }
        state.clear();
        require(!state.frame(0).visible() && !state.frame(1).visible(),
                "Respawn/logout must clear current and interpolated visual state");
        state.tick(0, 0);
        require(!state.frame(1).visible(), "Cleared filters must not revive old effects");
    }

    private static VitalSignsVisualState.Frame settled(int heartRate, float distress) {
        VitalSignsVisualState state = new VitalSignsVisualState();
        for (int tick = 0; tick < 100; tick++) {
            state.tick(heartRate, distress);
        }
        return state.frame(1);
    }

    private static void close(float expected, float actual, String message) {
        require(Math.abs(expected - actual) < 0.0002F, message + ": " + actual);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
