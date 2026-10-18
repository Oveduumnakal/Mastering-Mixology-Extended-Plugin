package work.fking.masteringmixology;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class StationGuardTest {

    @Test
    public void stationFromTargetStripsTagsAndMatchesNames() {
        assertEquals(AlchemyObject.RETORT, StationGuard.stationFromTarget("<col=00ff00>Retort</col>"));
        assertEquals(AlchemyObject.ALEMBIC, StationGuard.stationFromTarget("Alembic"));
        assertEquals(AlchemyObject.AGITATOR, StationGuard.stationFromTarget("Agitator"));
        assertNull(StationGuard.stationFromTarget("Bank booth"));
        assertNull(StationGuard.stationFromTarget(null));
    }

    @Test
    public void occupiedStationIsAlwaysNeeded() {
        assertTrue(StationGuard.isStationNeeded(
                AlchemyObject.RETORT, true, Collections.emptyList(), Collections.emptySet()));
    }

    @Test
    public void emptyStationNeededWhenAnUnfulfilledOrderMatchesAHeldPotion() {
        PotionType type = firstConcentrateType();
        PotionOrder order = new PotionOrder(0, type, PotionModifier.CONCENTRATED);
        List<PotionOrder> orders = Collections.singletonList(order);
        Set<Integer> held = new HashSet<>();
        held.add(type.itemId());

        assertTrue(StationGuard.isStationNeeded(AlchemyObject.RETORT, false, orders, held));
    }

    @Test
    public void emptyStationNotNeededWhenOrderFulfilled() {
        PotionType type = firstConcentrateType();
        PotionOrder order = new PotionOrder(0, type, PotionModifier.CONCENTRATED);
        order.setFulfilled(true);
        List<PotionOrder> orders = Collections.singletonList(order);
        Set<Integer> held = new HashSet<>();
        held.add(type.itemId());

        assertFalse(StationGuard.isStationNeeded(AlchemyObject.RETORT, false, orders, held));
    }

    @Test
    public void emptyStationNotNeededWhenPotionNotHeld() {
        PotionType type = firstConcentrateType();
        PotionOrder order = new PotionOrder(0, type, PotionModifier.CONCENTRATED);
        List<PotionOrder> orders = Collections.singletonList(order);

        assertFalse(StationGuard.isStationNeeded(
                AlchemyObject.RETORT, false, orders, Collections.emptySet()));
    }

    @Test
    public void concentrateCapBlocksWhenOneMoreClickWouldComplete() {
        // clickDelta 2, max 16: 14 + 2 = 16 -> block at 14, not at 13
        assertFalse(StationGuard.concentrateCapReached(13, 2, 16));
        assertTrue(StationGuard.concentrateCapReached(14, 2, 16));
    }

    @Test
    public void concentrateCapAdaptsToLargerClickDelta() {
        // clickDelta 3, max 16: 13 + 3 = 16 -> block at 13
        assertFalse(StationGuard.concentrateCapReached(12, 3, 16));
        assertTrue(StationGuard.concentrateCapReached(13, 3, 16));
    }

    private static PotionType firstConcentrateType() {
        // Any potion type works; the modifier (CONCENTRATED) drives the station.
        return PotionType.values()[0];
    }
}
