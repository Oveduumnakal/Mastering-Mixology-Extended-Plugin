package work.fking.masteringmixology;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.runelite.api.Client;

/**
 * The reward goal behind the overlay: the rewards ticked in Reward Tracking, their summed resin cost and how close
 * the player's current resin is to it.
 */
public class Goal
{
	private RewardItem rewardItem;
	private String displayName;
	private boolean multiMode = false;
	private double overallProgress = 0.0;
	private int itemsAffordable = 0;
	private int rewardQuantity = 1;

	/** Rebuilt for every component each time the goal is recalculated, so the values are never stale. */
	private final Map<PotionComponent, ComponentData> componentDataMap = new EnumMap<>(PotionComponent.class);

	public Goal(RewardItem rewardItem)
	{
		this.rewardItem = rewardItem;
		this.displayName = rewardItem.itemName();
	}

	public RewardItem getRewardItem()
	{
		return rewardItem;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public boolean isMultiMode()
	{
		return multiMode;
	}

	public double getOverallProgress()
	{
		return overallProgress;
	}

	public int getItemsAffordable()
	{
		return itemsAffordable;
	}

	public int getRewardQuantity()
	{
		return rewardQuantity;
	}

	public ComponentData getComponentData(PotionComponent component)
	{
		return componentDataMap.get(component);
	}

	/**
	 * Rebuilds the goal from the Reward Tracking settings and the player's current resin.
	 *
	 * <p>The cost of every ticked reward is summed per resin colour and the first one supplies the icon. The affordable
	 * count is limited by the scarcest colour, and the overall progress is the average of the colours' progress.
	 *
	 * @param config the plugin config holding the ticked rewards and their quantities
	 * @param client the client the current resin amounts are read from
	 */
	public void recalculate(MasteringMixologyConfig config, Client client)
	{
		List<Selection> selected = collectSelections(config);

		if (!selected.isEmpty())
		{
			multiMode = true;
			rewardItem = selected.get(0).item;
			displayName = selected.size() == 1
					? formatSingleDisplayName(selected.get(0))
					: "Selected rewards (" + selected.size() + ")";
			rewardQuantity = 1;

			for (var component : PotionComponent.ENTRIES)
			{
				int currentAmount = client.getVarpValue(component.resinVarpId());
				int summedCost = 0;
				for (Selection sel : selected)
					summedCost += sel.item.componentCost(component) * sel.quantity;

				componentDataMap.put(component, new ComponentData(currentAmount, summedCost, 1));
			}
		}
		else
		{
			multiMode = false;
			rewardItem = RewardItem.NONE;
			displayName = "";
			rewardQuantity = 1;

			for (var component : PotionComponent.ENTRIES)
			{
				int currentAmount = client.getVarpValue(component.resinVarpId());
				componentDataMap.put(component, new ComponentData(currentAmount, 0, 1));
			}
		}

		int minAffordable = componentDataMap.values().stream()
				.mapToInt(data -> data.affordableAmount)
				.min()
				.orElse(0);
		itemsAffordable = Math.min(minAffordable, rewardQuantity);

		overallProgress = componentDataMap.values().stream()
				.mapToDouble(data -> data.percentageToGoal)
				.average()
				.orElse(0.0);
	}

	private static String formatSingleDisplayName(Selection sel)
	{
		if (sel.quantity > 1)
			return sel.item.itemName() + " x" + sel.quantity;

		return sel.item.itemName();
	}

	static List<Selection> collectSelections(MasteringMixologyConfig config)
	{
		List<Selection> list = new ArrayList<>();
		if (config.trackPrescriptionGoggles())
			list.add(new Selection(RewardItem.PRESCRIPTION_GOGGLES, 1));

		if (config.trackAlchemistLabcoat())
			list.add(new Selection(RewardItem.ALCHEMIST_LABCOAT, 1));

		if (config.trackAlchemistPants())
			list.add(new Selection(RewardItem.ALCHEMIST_PANTS, 1));

		if (config.trackAlchemistGloves())
			list.add(new Selection(RewardItem.ALCHEMIST_GLOVES, 1));

		if (config.trackReagentPouch())
			list.add(new Selection(RewardItem.REAGENT_POUCH, 1));

		if (config.trackPotionStorage())
			list.add(new Selection(RewardItem.POTION_STORAGE, 1));

		if (config.chuggingBarrelQuantity() > 0)
			list.add(new Selection(RewardItem.CHUGGING_BARREL, config.chuggingBarrelQuantity()));

		if (config.trackAlchemistsAmulet())
			list.add(new Selection(RewardItem.ALCHEMISTS_AMULET, 1));

		if (config.apprenticePackQuantity() > 0)
			list.add(new Selection(RewardItem.APPRENTICE_POTION_PACK, config.apprenticePackQuantity()));

		if (config.adeptPackQuantity() > 0)
			list.add(new Selection(RewardItem.ADEPT_POTION_PACK, config.adeptPackQuantity()));

		if (config.expertPackQuantity() > 0)
			list.add(new Selection(RewardItem.EXPERT_POTION_PACK, config.expertPackQuantity()));

		if (config.aldariumQuantity() > 0)
			list.add(new Selection(RewardItem.ALDARIUM, config.aldariumQuantity()));

		return list;
	}

	/** A ticked reward and how many of it are wanted. */
	static final class Selection
	{
		final RewardItem item;
		final int quantity;

		Selection(RewardItem item, int quantity)
		{
			this.item = item;
			this.quantity = quantity;
		}
	}

	/** One resin colour's stock against the summed goal, with the progress and affordable count derived from them. */
	public static class ComponentData
	{
		final int currentAmount;
		final int goalAmount;
		final double percentageToGoal;
		final int affordableAmount;

		ComponentData(int currentAmount, int baseGoalAmount, int rewardQuantity)
		{
			this.currentAmount = currentAmount;
			this.goalAmount = baseGoalAmount * rewardQuantity;

			if (goalAmount == 0)
			{
				this.percentageToGoal = 1.0;
				this.affordableAmount = rewardQuantity;
			}
			else
			{
				this.percentageToGoal = Math.min((double) currentAmount / goalAmount, 1.0);
				this.affordableAmount = currentAmount / baseGoalAmount;
			}
		}
	}
}
