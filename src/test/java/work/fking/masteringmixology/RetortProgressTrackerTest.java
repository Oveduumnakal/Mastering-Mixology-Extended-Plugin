package work.fking.masteringmixology;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RetortProgressTrackerTest {

    @Test
    public void ignoresProgressWhenNoPotionLoaded() {
        RetortProgressTracker tracker = new RetortProgressTracker();
        tracker.onProgressChanged(5);
        assertEquals(0, tracker.currentProgress());
        assertFalse(tracker.potionLoaded());
    }

    @Test
    public void tracksCurrentProgressWhilePotionLoaded() {
        RetortProgressTracker tracker = new RetortProgressTracker();
        tracker.onPotionLoaded();
        tracker.onProgressChanged(2);
        tracker.onProgressChanged(4);
        assertTrue(tracker.potionLoaded());
        assertEquals(4, tracker.currentProgress());
    }

    @Test
    public void clickDeltaDefaultsToTwo() {
        RetortProgressTracker tracker = new RetortProgressTracker();
        assertEquals(2, tracker.clickDelta());
    }

    @Test
    public void passivePlusOneTicksDoNotLowerClickDelta() {
        RetortProgressTracker tracker = new RetortProgressTracker();
        tracker.onPotionLoaded();
        tracker.onProgressChanged(1);  // +1 passive (from load)
        tracker.onProgressChanged(2);  // +1 passive
        assertEquals(2, tracker.clickDelta());
    }

    @Test
    public void clickDeltaRisesWhenALargerJumpIsObserved() {
        RetortProgressTracker tracker = new RetortProgressTracker();
        tracker.onPotionLoaded();
        tracker.onProgressChanged(3);  // +3 jump (higher-level click)
        assertEquals(3, tracker.clickDelta());
    }

    @Test
    public void clickDeltaPersistsAcrossPotionsButProgressResets() {
        RetortProgressTracker tracker = new RetortProgressTracker();
        tracker.onPotionLoaded();
        tracker.onProgressChanged(3);   // clickDelta -> 3
        tracker.onPotionCleared();
        assertEquals(0, tracker.currentProgress());
        assertEquals(3, tracker.clickDelta());
        tracker.onPotionLoaded();
        assertEquals(0, tracker.currentProgress());
        assertEquals(3, tracker.clickDelta());
    }
}
