package com.swampd.superficialtrauma.common.qte;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TimingQteTimingTest {
    private static final long MS = 1_000_000L;
    private static TimingQteSnapshot snapshot() {
        return new TimingQteSnapshot(1, 1234567890123L, 26, .28F, .34F, .52F);
    }

    @Test void fractionalVisibleHitIsNotRoundedDownToEarlyFailure() {
        var qte = snapshot();
        var clock = new TimingQteTimeline(10);
        clock.present(0);
        float shown = clock.present(865 * MS); // 7.3 ticks, perfect starts at 7.28; flooring used to fail.
        assertEquals(TimingQteResult.EARLY_FAILURE, qte.classifyPress((float) Math.floor(shown)));
        assertEquals(TimingQteResult.PERFECT, qte.classifyPress(clock.inputElapsed(870 * MS)));
        assertEquals(shown, clock.inputElapsed(870 * MS));
    }

    @Test void firstVisibleFrameOwnsLeadInAndFullSweepIsStill1300ms() {
        var clock = new TimingQteTimeline(10);
        assertFalse(clock.started());
        assertEquals(-10, clock.present(10_000 * MS));
        assertEquals(0, clock.present(10_500 * MS));
        assertEquals(26, clock.present(11_800 * MS));
    }

    @Test void framesAreSmoothWithoutWorldTicksAndOldWorldTimesRetainFractions() {
        var clock = new TimingQteTimeline(0);
        clock.present(0);
        assertEquals(.32F, clock.present(16 * MS), .00001F);
        assertEquals(.64F, clock.present(32 * MS), .00001F);
        var qte = snapshot();
        assertEquals(7.75F, qte.elapsedTicksAt(qte.cursorStartGameTime() + 7, .75F));
    }

    @Test void allScoringBoundariesRemainHalfOpen() {
        var qte = snapshot();
        assertEquals(TimingQteResult.EARLY_FAILURE, qte.classifyPress(qte.perfectStartTick() - .0001F));
        assertEquals(TimingQteResult.PERFECT, qte.classifyPress(qte.perfectStartTick()));
        assertEquals(TimingQteResult.PERFECT, qte.classifyPress(qte.normalStartTick() - .0001F));
        assertEquals(TimingQteResult.SUCCESS, qte.classifyPress(qte.normalStartTick()));
        assertEquals(TimingQteResult.SUCCESS, qte.classifyPress(qte.successEndTick() - .0001F));
        assertEquals(TimingQteResult.MISSED_FAILURE, qte.classifyPress(qte.successEndTick()));
    }

    @Test void tickCannotExpireAStillVisibleValidFrameButStallsAreBounded() {
        var qte = snapshot();
        var clock = new TimingQteTimeline(0);
        clock.present(0);
        float shown = clock.present(670 * MS);
        assertEquals(TimingQteResult.SUCCESS, qte.classifyPress(shown));
        assertFalse(clock.shouldMiss(qte.successEndTick(), 680 * MS));
        assertEquals(shown, clock.inputElapsed(680 * MS));
        assertTrue(clock.shouldMiss(qte.successEndTick(), 921 * MS));
        assertEquals(TimingQteResult.MISSED_FAILURE, qte.classifyPress(clock.inputElapsed(921 * MS)));
    }

    @Test void constantNetworkLatencyDoesNotShiftJudgement() {
        for (long oneWayMs : new long[]{0, 25, 75, 200, 400, 800}) {
            var qte = snapshot();
            var server = new TimingQteServerWindow(qte, 10, 8, 0);
            long readyArrival = 2 * oneWayMs * MS + 16 * MS;
            assertTrue(server.ready(readyArrival));
            for (float elapsed : new float[]{-5, 7, 7.5F, 10.4F, 13.52F}) {
                long arrival = readyArrival + (long) ((10D + elapsed) * 50 * MS) + 8 * MS;
                assertTrue(server.accepts(elapsed, arrival), "one-way latency " + oneWayMs);
            }
        }
    }

    @Test void jitterAndFrameAgeAreToleratedWithoutMovingReportedPosition() {
        var server = new TimingQteServerWindow(snapshot(), 10, 8, 0);
        assertTrue(server.ready(150 * MS));
        assertTrue(server.accepts(7.3F, 150 * MS + 865 * MS + 400 * MS));
        assertEquals(TimingQteResult.PERFECT, snapshot().classifyPress(7.3F));
        assertFalse(server.accepts(7.3F, 150 * MS)); // Cannot instantly claim a future success.
    }

    @Test void readinessAndSessionExpiryAreBoundedAndCannotBeRenewed() {
        var server = new TimingQteServerWindow(snapshot(), 10, 8, 0);
        assertFalse(server.accepts(10, 100 * MS));
        assertTrue(server.ready(100 * MS));
        assertFalse(server.ready(1000 * MS));
        assertTrue(server.expired(2000 * MS));
        assertFalse(server.accepts(10, 2000 * MS));
        var unready = new TimingQteServerWindow(snapshot(), 10, 8, 0);
        assertTrue(unready.expired(5001 * MS));
        assertFalse(unready.ready(5001 * MS));
        var slow = new TimingQteServerWindow(snapshot(), 10, 8, 0);
        assertTrue(slow.ready(4900 * MS));
        assertTrue(slow.expired(9000 * MS));
    }

    @Test void invalidNumbersNeverScore() {
        var server = new TimingQteServerWindow(snapshot(), 10, 8, 0);
        server.ready(0);
        for (float invalid : new float[]{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, -11, 100}) {
            assertFalse(server.accepts(invalid, 900 * MS));
        }
        assertEquals(TimingQteResult.MISSED_FAILURE, snapshot().classifyPress(Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> new TimingQteSnapshot(1, 0, 26, Float.NaN, .3F, .5F));
        assertThrows(IllegalArgumentException.class, () -> new TimingQteTimeline(-1));
    }
}
