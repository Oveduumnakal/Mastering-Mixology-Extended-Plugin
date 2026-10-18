package work.fking.masteringmixology;

/**
 * The extra processing step an order calls for, with the station that does it and the experience its quick action
 * gives.
 */
public enum PotionModifier
{
	/** Agitated. Clicking the quick-time event on the Agitator gives 14 experience, and it can happen once or twice. */
	HOMOGENOUS(AlchemyObject.AGITATOR, 21),
	/** Concentrated. Each click on the Retort gives 2 experience, for a maximum of 10 clicks. */
	CONCENTRATED(AlchemyObject.RETORT, 20),
	/** Crystallised. Clicking the quick-time event on the Alembic gives 14 experience. */
	CRYSTALISED(AlchemyObject.ALEMBIC, 14);

	private static final PotionModifier[] TYPES = PotionModifier.values();

	private final AlchemyObject alchemyObject;
	private final int quickActionExperience;

	PotionModifier(AlchemyObject alchemyObject, int quickActionExperience)
	{
		this.alchemyObject = alchemyObject;
		this.quickActionExperience = quickActionExperience;
	}

	public static PotionModifier from(int potionModifierId)
	{
		if (potionModifierId < 0 || potionModifierId >= TYPES.length)
			return null;

		return TYPES[potionModifierId];
	}

	public AlchemyObject alchemyObject()
	{
		return alchemyObject;
	}

	public int quickActionExperience()
	{
		return quickActionExperience;
	}
}
