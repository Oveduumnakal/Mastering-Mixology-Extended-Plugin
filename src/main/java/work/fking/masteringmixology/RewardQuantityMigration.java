package work.fking.masteringmixology;

import net.runelite.client.config.ConfigManager;

import static work.fking.masteringmixology.MasteringMixologyConfig.CONFIG_GROUP;

/**
 * Moves the repeatable rewards from a tick box plus a quantity to a single quantity, where 0 means not tracked.
 *
 * <p>A ticked reward keeps its quantity (at least 1); an unticked one drops to 0. The old tick box keys are removed
 * and a marker key stops the migration from running again, so a quantity set afterwards is never overwritten.
 */
final class RewardQuantityMigration
{
	static final String MIGRATED_KEY = "rewardQuantityMigrated";

	/** Each pair is the old tick box key and the quantity key that replaces it. */
	private static final String[][] KEY_PAIRS = {
			{"trackChuggingBarrel", "chuggingBarrelQuantity"},
			{"trackApprenticePack", "apprenticePackQuantity"},
			{"trackAdeptPack", "adeptPackQuantity"},
			{"trackExpertPack", "expertPackQuantity"},
			{"trackAldarium", "aldariumQuantity"},
	};

	private RewardQuantityMigration()
	{
	}

	static void migrate(ConfigManager configManager)
	{
		if (configManager.getConfiguration(CONFIG_GROUP, MIGRATED_KEY) != null)
			return;

		for (String[] pair : KEY_PAIRS)
		{
			int quantity = migratedQuantity(
					configManager.getConfiguration(CONFIG_GROUP, pair[0]),
					configManager.getConfiguration(CONFIG_GROUP, pair[1]));

			if (quantity == 0)
				configManager.unsetConfiguration(CONFIG_GROUP, pair[1]);
			else
				configManager.setConfiguration(CONFIG_GROUP, pair[1], quantity);

			configManager.unsetConfiguration(CONFIG_GROUP, pair[0]);
		}

		configManager.setConfiguration(CONFIG_GROUP, MIGRATED_KEY, true);
	}

	/**
	 * @param tracked  the stored tick box value, or null if it was never set
	 * @param quantity the stored quantity, or null if it was never set (the old default was 1)
	 * @return the quantity to keep: 0 when the reward was not ticked, otherwise the old quantity, at least 1
	 */
	static int migratedQuantity(String tracked, String quantity)
	{
		if (!Boolean.parseBoolean(tracked))
			return 0;

		if (quantity == null)
			return 1;

		try
		{
			return Math.max(1, Integer.parseInt(quantity.trim()));
		}
		catch (NumberFormatException e)
		{
			return 1;
		}
	}
}
