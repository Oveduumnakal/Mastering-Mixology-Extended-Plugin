package work.fking.masteringmixology;

class RetortProgressTracker {

    // The cap stops one click short of completion: block when
    // currentProgress + clickDelta >= max. clickDelta is the largest progress
    // jump seen on a click tick (passive ticks add +1, a click adds +2 at the
    // measured level). It defaults to 2 and only rises, so a higher Herblore
    // level that makes a click worth +3 is picked up automatically. It persists
    // across potions (it is a property of the player's level), while
    // currentProgress resets per potion.
    private boolean potionLoaded;
    private int currentProgress;
    private int clickDelta = 2;

    void onPotionLoaded() {
        potionLoaded = true;
        currentProgress = 0;
    }

    void onPotionCleared() {
        potionLoaded = false;
        currentProgress = 0;
    }

    void onProgressChanged(int value) {
        if (!potionLoaded) {
            return;
        }
        int delta = value - currentProgress;
        if (delta > clickDelta) {
            clickDelta = delta;
        }
        currentProgress = value;
    }

    int currentProgress() {
        return currentProgress;
    }

    int clickDelta() {
        return clickDelta;
    }

    boolean potionLoaded() {
        return potionLoaded;
    }
}
