package work.fking.masteringmixology;

import java.util.List;
import java.util.Set;
import net.runelite.client.util.Text;

final class StationGuard {

    // Maximum value of VarbitID.MM_RETORT_PROGRESS (fixed game constant; the bar
    // runs 0..16, then resets to 0 on completion).
    static final int RETORT_PROGRESS_MAX = 16;

    private StationGuard() {
    }

    static AlchemyObject stationFromTarget(String rawTarget) {
        if (rawTarget == null) {
            return null;
        }
        String target = Text.removeTags(rawTarget);
        if (target.equals("Alembic")) {
            return AlchemyObject.ALEMBIC;
        }
        if (target.equals("Agitator")) {
            return AlchemyObject.AGITATOR;
        }
        if (target.equals("Retort")) {
            return AlchemyObject.RETORT;
        }
        return null;
    }

    static boolean isStationNeeded(AlchemyObject station, boolean occupied, List<PotionOrder> orders, Set<Integer> heldItemIds) {
        if (occupied) {
            return true;
        }
        for (PotionOrder order : orders) {
            if (!order.fulfilled()
                    && order.potionModifier().alchemyObject() == station
                    && heldItemIds.contains(order.potionType().itemId())) {
                return true;
            }
        }
        return false;
    }

    static boolean concentrateCapReached(int currentProgress, int clickDelta, int maxProgress) {
        // Block once one more click would reach/exceed the max; the bar then
        // coasts the last step(s) to completion on passive fill.
        return currentProgress + clickDelta >= maxProgress;
    }
}
