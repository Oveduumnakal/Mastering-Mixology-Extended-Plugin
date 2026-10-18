package work.fking.masteringmixology;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Tests for {@link RetortProgressTracker}. */
public class RetortProgressTrackerTest
{
	@Test
	public void ignoresProgressWhenNoPotionLoaded()
	{
		RetortProgressTracker tracker = new RetortProgressTracker();
		tracker.onProgressChanged(5);
		assertEquals(0, tracker.currentProgress());
		assertFalse(tracker.potionLoaded());
	}

	@Test
	public void tracksCurrentProgressWhilePotionLoaded()
	{
		RetortProgressTracker tracker = new RetortProgressTracker();
		tracker.onPotionLoaded();
		tracker.onProgressChanged(2);
		tracker.onProgressChanged(4);
		assertTrue(tracker.potionLoaded());
		assertEquals(4, tracker.currentProgress());
	}

	@Test
	public void clickDeltaDefaultsToTwo()
	{
		RetortProgressTracker tracker = new RetortProgressTracker();
		assertEquals(2, tracker.clickDelta());
	}

	/** Progress rising by 1 per tick, as passive fill does, leaves the click delta at its default of 2. */
	@Test
	public void passivePlusOneTicksDoNotLowerClickDelta()
	{
		RetortProgressTracker tracker = new RetortProgressTracker();
		tracker.onPotionLoaded();
		tracker.onProgressChanged(1);
		tracker.onProgressChanged(2);
		assertEquals(2, tracker.clickDelta());
	}

	/** A jump of 3, as a higher-level click makes, raises the click delta to 3. */
	@Test
	public void clickDeltaRisesWhenALargerJumpIsObserved()
	{
		RetortProgressTracker tracker = new RetortProgressTracker();
		tracker.onPotionLoaded();
		tracker.onProgressChanged(3);
		assertEquals(3, tracker.clickDelta());
	}

	/** The click delta belongs to the player's level, so it survives a cleared potion while the progress resets. */
	@Test
	public void clickDeltaPersistsAcrossPotionsButProgressResets()
	{
		RetortProgressTracker tracker = new RetortProgressTracker();
		tracker.onPotionLoaded();
		tracker.onProgressChanged(3);
		tracker.onPotionCleared();
		assertEquals(0, tracker.currentProgress());
		assertEquals(3, tracker.clickDelta());
		tracker.onPotionLoaded();
		assertEquals(0, tracker.currentProgress());
		assertEquals(3, tracker.clickDelta());
	}
}
