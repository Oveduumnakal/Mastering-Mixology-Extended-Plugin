package work.fking.masteringmixology;

/**
 * Tracks the Retort's progress bar so concentrate clicks can be capped one click short of completion.
 *
 * <p>The cap blocks when {@code currentProgress + clickDelta >= max}. The click delta is the largest progress jump
 * seen on a click tick: passive ticks add 1 and a click adds 2 at the measured level. It starts at 2 and only rises,
 * so a higher Herblore level that makes a click worth 3 is picked up automatically. It persists across potions
 * because it is a property of the player's level, while the current progress resets for each potion.
 */
class RetortProgressTracker
{
	private boolean potionLoaded;
	private int currentProgress;
	private int clickDelta = 2;

	void onPotionLoaded()
	{
		potionLoaded = true;
		currentProgress = 0;
	}

	void onPotionCleared()
	{
		potionLoaded = false;
		currentProgress = 0;
	}

	void onProgressChanged(int value)
	{
		if (!potionLoaded)
			return;

		int delta = value - currentProgress;
		if (delta > clickDelta)
			clickDelta = delta;

		currentProgress = value;
	}

	int currentProgress()
	{
		return currentProgress;
	}

	int clickDelta()
	{
		return clickDelta;
	}

	boolean potionLoaded()
	{
		return potionLoaded;
	}
}
