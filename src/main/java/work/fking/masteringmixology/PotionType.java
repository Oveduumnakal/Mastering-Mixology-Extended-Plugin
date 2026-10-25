package work.fking.masteringmixology;

import java.util.Arrays;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import net.runelite.api.gameval.ItemID;

import static work.fking.masteringmixology.PotionComponent.AGA;
import static work.fking.masteringmixology.PotionComponent.LYE;
import static work.fking.masteringmixology.PotionComponent.MOX;

/** Every potion type, with its unfinished and finished item ids and its recipe of three resin components. */
public enum PotionType
{
	MAMMOTH_MIGHT_MIX(ItemID.MM_POTION_MMM_UNFINISHED, ItemID.MM_POTION_MMM_FINISHED, 1900, MOX, MOX, MOX),
	MYSTIC_MANA_AMALGAM(ItemID.MM_POTION_MMA_UNFINISHED, ItemID.MM_POTION_MMA_FINISHED, 2150, MOX, MOX, AGA),
	MARLEYS_MOONLIGHT(ItemID.MM_POTION_MML_UNFINISHED, ItemID.MM_POTION_MML_FINISHED, 2400, MOX, MOX, LYE),
	ALCO_AUGMENTATOR(ItemID.MM_POTION_AAA_UNFINISHED, ItemID.MM_POTION_AAA_FINISHED, 1900, AGA, AGA, AGA),
	AZURE_AURA_MIX(ItemID.MM_POTION_AAM_UNFINISHED, ItemID.MM_POTION_AAM_FINISHED, 2650, AGA, AGA, MOX),
	AQUALUX_AMALGAM(ItemID.MM_POTION_AAL_UNFINISHED, ItemID.MM_POTION_AAL_FINISHED, 2900, AGA, LYE, AGA),
	LIPLACK_LIQUOR(ItemID.MM_POTION_LLL_UNFINISHED, ItemID.MM_POTION_LLL_FINISHED, 1900, LYE, LYE, LYE),
	MEGALITE_LIQUID(ItemID.MM_POTION_LLM_UNFINISHED, ItemID.MM_POTION_LLM_FINISHED, 3150, MOX, LYE, LYE),
	ANTI_LEECH_LOTION(ItemID.MM_POTION_LLA_UNFINISHED, ItemID.MM_POTION_LLA_FINISHED, 3400, AGA, LYE, LYE),
	MIXALOT(ItemID.MM_POTION_MAL_UNFINISHED, ItemID.MM_POTION_MAL_FINISHED, 3650, MOX, AGA, LYE);

	public static final PotionType[] TYPES = PotionType.values();

	private static final Map<Integer, PotionType> ITEM_MAP;

	static
	{
		var builder = new ImmutableMap.Builder<Integer, PotionType>();
		for (var p : PotionType.values())
		{
			builder.put(p.itemId(), p);
			builder.put(p.modifiedItemId(), p);
		}

		ITEM_MAP = builder.build();
	}

	private final int itemId;
	private final int modifiedItemId;
	private final String recipe;
	private final String abbreviation;
	private final int experience;
	private final PotionComponent[] components;
	private final int[] resinYield;
	private final int orderWeight;

	PotionType(int itemId, int modifiedItemId, int experience, PotionComponent... components)
	{
		this.itemId = itemId;
		this.modifiedItemId = modifiedItemId;
		this.recipe = colorizeRecipe(components);
		this.experience = experience;
		this.components = components;
		this.abbreviation = "" + components[0].character() + components[1].character() + components[2].character();
		this.resinYield = resinYield(components);
		this.orderWeight = 6 - distinctColours(components);
	}

	/**
	 * Works out the resin an ordered potion gives per colour: 20 of a colour used two or three times, 10 of the single
	 * colour in a two-colour potion, and 20 of each colour in Mixalot.
	 */
	private static int[] resinYield(PotionComponent[] components)
	{
		int[] count = new int[PotionComponent.ENTRIES.length];
		for (var component : components)
			count[component.ordinal()]++;

		boolean twoColours = distinctColours(components) == 2;
		int[] yield = new int[count.length];
		for (int c = 0; c < count.length; c++)
		{
			if (count[c] == 0)
				continue;

			yield[c] = count[c] == 1 && twoColours ? 10 : 20;
		}

		return yield;
	}

	private static int distinctColours(PotionComponent[] components)
	{
		return (int) Arrays.stream(components)
				.distinct()
				.count();
	}

	public static PotionType fromItemId(int itemId)
	{
		return ITEM_MAP.get(itemId);
	}

	public static PotionType fromIdx(int potionTypeId)
	{
		if (potionTypeId < 0 || potionTypeId >= TYPES.length)
			return null;

		return TYPES[potionTypeId];
	}

	private static String colorizeRecipe(PotionComponent[] components)
	{
		if (components.length != 3)
			throw new IllegalArgumentException("Invalid potion components: " + Arrays.toString(components));

		return colorizeRecipeComponent(components[0])
				+ colorizeRecipeComponent(components[1])
				+ colorizeRecipeComponent(components[2]);
	}

	private static String colorizeRecipeComponent(PotionComponent component)
	{
		return "<col=" + component.colorCode() + ">" + component.character() + "</col>";
	}

	public String recipeBlocks(int[] iconIdx)
	{
		StringBuilder sb = new StringBuilder(18);
		for (PotionComponent c : components)
			sb.append("<img=")
					.append(iconIdx[c.ordinal()])
					.append('>');

		return sb.toString();
	}

	public int itemId()
	{
		return itemId;
	}

	public int modifiedItemId()
	{
		return modifiedItemId;
	}

	public String recipe()
	{
		return recipe;
	}

	public int experience()
	{
		return experience;
	}

	public PotionComponent[] components()
	{
		return components;
	}

	/** Returns the base resin of the given colour index (0 mox, 1 aga, 2 lye) that fulfilling an order gives. */
	public int resinYield(int colour)
	{
		return resinYield[colour];
	}

	/**
	 * Returns the potion's relative chance of being ordered: 5 for single-colour potions, 4 for two-colour potions and
	 * 3 for Mixalot, as listed on the OSRS Wiki.
	 */
	public int orderWeight()
	{
		return orderWeight;
	}

	/**
	 * Returns the potion's name split into the three parts that stand for its three components, in recipe order.
	 * Joining the parts gives the name. Each part starts with its component's letter, except in Mixalot ("ot").
	 */
	public String[] nameParts()
	{
		switch (this)
		{
			case MAMMOTH_MIGHT_MIX: return new String[]{"Mammoth", "-might ", "mix"};
			case MYSTIC_MANA_AMALGAM: return new String[]{"Mystic ", "mana ", "amalgam"};
			case MARLEYS_MOONLIGHT: return new String[]{"Marley's ", "moon", "light"};
			case ALCO_AUGMENTATOR: return new String[]{"Alco", "-augment", "ator"};
			case AZURE_AURA_MIX: return new String[]{"Azure ", "aura ", "mix"};
			case AQUALUX_AMALGAM: return new String[]{"Aqua", "lux ", "amalgam"};
			case LIPLACK_LIQUOR: return new String[]{"Lip", "lack ", "liquor"};
			case MEGALITE_LIQUID: return new String[]{"Mega", "lite ", "liquid"};
			case ANTI_LEECH_LOTION: return new String[]{"Anti", "-leech ", "lotion"};
			default: return new String[]{"Mix", "al", "ot"};
		}
	}

	public String abbreviation()
	{
		return abbreviation;
	}
}
