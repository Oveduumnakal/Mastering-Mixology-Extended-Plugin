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

/** Tests for {@link StationGuard}. */
public class StationGuardTest
{
	@Test
	public void stationFromTargetStripsTagsAndMatchesNames()
	{
		assertEquals(AlchemyObject.RETORT, StationGuard.stationFromTarget("<col=00ff00>Retort</col>"));
		assertEquals(AlchemyObject.ALEMBIC, StationGuard.stationFromTarget("Alembic"));
		assertEquals(AlchemyObject.AGITATOR, StationGuard.stationFromTarget("Agitator"));
		assertNull(StationGuard.stationFromTarget("Bank booth"));
		assertNull(StationGuard.stationFromTarget(null));
	}

	@Test
	public void occupiedStationIsAlwaysNeeded()
	{
		assertTrue(StationGuard.isStationNeeded(
				AlchemyObject.RETORT, true, Collections.emptyList(), Collections.emptySet()));
	}

	@Test
	public void emptyStationNeededWhenAnUnfulfilledOrderMatchesAHeldPotion()
	{
		PotionType type = firstConcentrateType();
		PotionOrder order = new PotionOrder(0, type, PotionModifier.CONCENTRATED);
		List<PotionOrder> orders = Collections.singletonList(order);
		Set<Integer> held = new HashSet<>();
		held.add(type.itemId());

		assertTrue(StationGuard.isStationNeeded(AlchemyObject.RETORT, false, orders, held));
	}

	@Test
	public void emptyStationNotNeededWhenOrderFulfilled()
	{
		PotionType type = firstConcentrateType();
		PotionOrder order = new PotionOrder(0, type, PotionModifier.CONCENTRATED);
		order.setFulfilled(true);
		List<PotionOrder> orders = Collections.singletonList(order);
		Set<Integer> held = new HashSet<>();
		held.add(type.itemId());

		assertFalse(StationGuard.isStationNeeded(AlchemyObject.RETORT, false, orders, held));
	}

	@Test
	public void emptyStationNotNeededWhenPotionNotHeld()
	{
		PotionType type = firstConcentrateType();
		PotionOrder order = new PotionOrder(0, type, PotionModifier.CONCENTRATED);
		List<PotionOrder> orders = Collections.singletonList(order);

		assertFalse(StationGuard.isStationNeeded(
				AlchemyObject.RETORT, false, orders, Collections.emptySet()));
	}

	/**
	 * With a click delta of 2 and a maximum of 16, 14 + 2 reaches the maximum, so the cap blocks at 14 and not
	 * at 13.
	 */
	@Test
	public void concentrateCapBlocksWhenOneMoreClickWouldComplete()
	{
		assertFalse(StationGuard.concentrateCapReached(13, 2, 16));
		assertTrue(StationGuard.concentrateCapReached(14, 2, 16));
	}

	/** With a click delta of 3 and a maximum of 16, 13 + 3 reaches the maximum, so the cap blocks at 13. */
	@Test
	public void concentrateCapAdaptsToLargerClickDelta()
	{
		assertFalse(StationGuard.concentrateCapReached(12, 3, 16));
		assertTrue(StationGuard.concentrateCapReached(13, 3, 16));
	}

	/** Any potion type works, because the modifier (concentrated) drives the station. */
	private static PotionType firstConcentrateType()
	{
		return PotionType.values()[0];
	}
}
