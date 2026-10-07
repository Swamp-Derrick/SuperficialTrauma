package com.swampd.superficialtrauma.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConcussionVisualStateTest {
    @Test void strongerBlurKeepsCoverageAndPulseTimingAndScalesWithResolution() {
        var state = new ConcussionVisualState();
        assertEquals(24, state.blurRadiusPixels(1080));
        state.onset(); state.update(.05, true, false, 0);
        assertEquals(64, state.blurRadiusPixels(1080));
        assertEquals(32, state.blurRadiusPixels(540));
        for (int i = 0; i < 3; i++) state.update(.1, true, false, 0);
        assertEquals(24, state.blurRadiusPixels(1080));
        assertEquals(.25, ConcussionVisualState.targetCoverage(0, false));
    }
    @Test void coverageAnchorsAndPainControl() {
        assertEquals(.25, ConcussionVisualState.targetCoverage(0, false));
        assertEquals(.4, ConcussionVisualState.targetCoverage(4.3, false), 1E-9);
        assertEquals(.8, ConcussionVisualState.targetCoverage(20, false), 1E-9);
        assertEquals(.05, ConcussionVisualState.targetCoverage(0, true));
        assertEquals(.2, ConcussionVisualState.targetCoverage(20, true), 1E-9);
    }
    @Test void circularMaskRepresentsAreaAcrossScreenAspectRatios() {
        for (double aspect : new double[]{.75, 1, 16D/9, 21D/9}) {
            for (double coverage : new double[]{.05, .2, .25, .4, .8}) {
                double a = Math.max(1, aspect) / 2, b = Math.max(1, 1 / aspect) / 2;
                double radius = ConcussionVisualState.clearRadius(coverage, aspect);
                assertEquals(1 - coverage, ConcussionVisualState.circleArea(radius, a, b) / (4 * a * b), 1E-5);
            }
        }
    }
    @Test void frameIndependentSmoothingAndCureFade() {
        var slow = new ConcussionVisualState(); var fast = new ConcussionVisualState();
        for (int i = 0; i < 30 * 5; i++) slow.update(1D/30, true, false, 6);
        for (int i = 0; i < 144 * 5; i++) fast.update(1D/144, true, false, 6);
        assertEquals(slow.coverage(), fast.coverage(), .001);
        double before = fast.coverage(); fast.update(1D/60, true, true, 0);
        assertTrue(fast.coverage() < before && fast.coverage() > .2);
        float intensity = fast.intensity(); fast.update(1D/60, false, false, 0);
        assertTrue(fast.intensity() < intensity && fast.intensity() > 0);
        for (int i = 0; i < 700; i++) fast.update(1D/60, false, false, 0);
        assertTrue(fast.intensity() < .001);
    }
    @Test void onsetFlashLastsPointThreeSecondsAndNeverRepeatsByItself() {
        var state = new ConcussionVisualState(); state.onset();
        state.update(.05, true, false, 0); assertEquals(1, state.pulse());
        state.update(.1, true, false, 0); assertEquals(1, state.pulse(), .00001);
        state.update(.1, true, false, 0); assertTrue(state.pulse() > 0 && state.pulse() < 1);
        state.update(.05, true, false, 0); assertEquals(0, state.pulse(), .00001);
        for (int i = 0; i < 100; i++) state.update(.1, true, false, 0);
        assertEquals(0, state.pulse());
    }
}
