package work.fking.masteringmixology;

import org.junit.Test;

import net.runelite.client.config.ConfigManager;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static work.fking.masteringmixology.MasteringMixologyConfig.CONFIG_GROUP;

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

	@Test
	public void migrateMovesTickBoxesIntoQuantitiesOnce()
	{
		ConfigManager configManager = mock(ConfigManager.class);
		when(configManager.getConfiguration(CONFIG_GROUP, "trackAldarium")).thenReturn("true");
		when(configManager.getConfiguration(CONFIG_GROUP, "aldariumQuantity")).thenReturn("40");
		when(configManager.getConfiguration(CONFIG_GROUP, "trackExpertPack")).thenReturn("false");
		when(configManager.getConfiguration(CONFIG_GROUP, "expertPackQuantity")).thenReturn("7");

		RewardQuantityMigration.migrate(configManager);

		verify(configManager).setConfiguration(CONFIG_GROUP, "aldariumQuantity", 40);
		verify(configManager).unsetConfiguration(CONFIG_GROUP, "expertPackQuantity");
		verify(configManager).unsetConfiguration(CONFIG_GROUP, "trackAldarium");
		verify(configManager).unsetConfiguration(CONFIG_GROUP, "trackExpertPack");
		verify(configManager).setConfiguration(CONFIG_GROUP, RewardQuantityMigration.MIGRATED_KEY, true);
	}

	/** Once the marker is set, later quantities are the player's own and are never touched again. */
	@Test
	public void migrateDoesNothingOnceMarked()
	{
		ConfigManager configManager = mock(ConfigManager.class);
		when(configManager.getConfiguration(CONFIG_GROUP, RewardQuantityMigration.MIGRATED_KEY)).thenReturn("true");

		RewardQuantityMigration.migrate(configManager);

		verify(configManager, never()).unsetConfiguration(anyString(), anyString());
		verify(configManager, never()).setConfiguration(anyString(), anyString(), anyInt());
	}
}
