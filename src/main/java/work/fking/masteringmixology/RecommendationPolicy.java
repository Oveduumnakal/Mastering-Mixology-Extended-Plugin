package work.fking.masteringmixology;

import java.util.Set;

/** Decides which of the three visible orders the player should brew this turn. */
public interface RecommendationPolicy
{
	/**
	 * Decides which slots to brew. Delivering any potion refreshes all three orders, so the slots left out are the
	 * ones to skip.
	 *
	 * @param orders the 3 current orders' PotionTypes (slot index = array index)
	 * @param resin  [mox, aga, lye] current resin from varps
	 * @param target [mox, aga, lye] total target (sum of selected reward costs)
	 * @return set of order indices (0..2) the player should brew this turn
	 */
	Set<Integer> decide(PotionType[] orders, int[] resin, int[] target);

	/** Clears any state carried between calls. Stateless policies keep this empty default. */
	default void reset()
	{
	}
}
