package work.fking.masteringmixology;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Tests for {@link RewardQuantityMigration}. */
public class RewardQuantityMigrationTest
{
	@Test
	public void untickedRewardDropsToZero()
	{
		assertEquals(0, RewardQuantityMigration.migratedQuantity(null, null));
		assertEquals(0, RewardQuantityMigration.migratedQuantity("false", "25"));
	}

	@Test
	public void tickedRewardKeepsItsQuantity()
	{
		assertEquals(25, RewardQuantityMigration.migratedQuantity("true", "25"));
	}

	/** A ticked reward whose quantity was never changed had the old default of 1. */
	@Test
	public void tickedRewardWithoutStoredQuantityBecomesOne()
	{
		assertEquals(1, RewardQuantityMigration.migratedQuantity("true", null));
	}

	@Test
	public void tickedRewardNeverDropsBelowOne()
	{
		assertEquals(1, RewardQuantityMigration.migratedQuantity("true", "0"));
		assertEquals(1, RewardQuantityMigration.migratedQuantity("true", "not a number"));
	}
}
