package work.fking.masteringmixology;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Recommends the delivery that gives the most useful resin per potion over the long run.
 *
 * <p>Every non-empty subset of the three orders is a possible delivery. Its useful resin is the resin it gives,
 * including the +20% (two orders) or +40% (three orders) batch bonus, with each colour capped at what is still needed
 * for the target. Each colour's resin is then weighted by its deficit relative to the largest one: every colour is
 * offered equally often, so the colour furthest behind takes longest to finish and its resin is worth the most.
 * Delivering refreshes all three orders, so each potion brewed now has an opportunity cost: the
 * useful resin per potion the player can expect from future order sets. That rate is found by Dinkelbach iteration
 * over every possible order set, weighted by the in-game order chances, and the delivery that beats it by the most
 * wins.
 */
public class ExpectedValuePolicy implements RecommendationPolicy
{
	private static final int[][] SUBSETS = {{0}, {1}, {2}, {0, 1}, {0, 2}, {1, 2}, {0, 1, 2}};
	private static final int MAX_ITERATIONS = 50;
	private static final double EPSILON = 1e-6;
	private static final List<Draw> DRAWS = draws();

	@Override
	public Set<Integer> decide(PotionType[] orders, int[] resin, int[] target)
	{
		int[] deficit = MetaPolicy.deficit(resin, target);
		if (deficit[0] == 0 && deficit[1] == 0 && deficit[2] == 0)
			return MetaPolicy.all3();

		double[] weight = weights(deficit);
		int[] best = bestSubset(orders, deficit, weight, longRunRate(deficit, weight));
		Set<Integer> slots = new HashSet<>();
		if (best != null)
		{
			for (int slot : best)
				slots.add(slot);
		}

		return slots;
	}

	/**
	 * Returns the weighted useful resin per potion that the best play earns on average from a fresh order set,
	 * holding the deficit fixed.
	 */
	static double longRunRate(int[] deficit, double[] weight)
	{
		double rate = 0;
		for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++)
		{
			double resin = 0;
			double potions = 0;
			for (Draw draw : DRAWS)
			{
				int[] best = bestSubset(draw.orders, deficit, weight, rate);
				resin += draw.chance * gain(draw.orders, best, deficit, weight);
				potions += draw.chance * best.length;
			}

			double next = resin / potions;
			if (Math.abs(next - rate) < EPSILON)
				return next;

			rate = next;
		}

		return rate;
	}

	/**
	 * Returns the delivery with the highest weighted useful resin minus the opportunity cost of its potions; a tie
	 * goes to the delivery with more useful resin. Returns null when no order is known.
	 */
	static int[] bestSubset(PotionType[] orders, int[] deficit, double[] weight, double rate)
	{
		int[] best = null;
		double bestScore = Double.NEGATIVE_INFINITY;
		double bestGain = -1;
		for (int[] subset : SUBSETS)
		{
			if (!known(orders, subset))
				continue;

			double gain = gain(orders, subset, deficit, weight);
			double score = gain - rate * subset.length;
			if (score > bestScore + EPSILON || (score > bestScore - EPSILON && gain > bestGain))
			{
				best = subset;
				bestScore = score;
				bestGain = gain;
			}
		}

		return best;
	}

	/**
	 * Returns the weighted useful resin of delivering the given slots together: the base resin times 1, 1.2 or 1.4 for
	 * one, two or three orders, each colour capped at its deficit and multiplied by its weight.
	 */
	static double gain(PotionType[] orders, int[] subset, int[] deficit, double[] weight)
	{
		int bonusFifths = 4 + subset.length;
		double total = 0;
		for (int colour = 0; colour < 3; colour++)
		{
			int resin = 0;
			for (int slot : subset)
				resin += orders[slot].resinYield(colour);

			total += weight[colour] * Math.min(deficit[colour], resin * bonusFifths / 5);
		}

		return total;
	}

	/** Weights each colour by its deficit relative to the largest one, from 0 (not needed) to 1. */
	static double[] weights(int[] deficit)
	{
		int max = Math.max(deficit[0], Math.max(deficit[1], deficit[2]));
		double[] weight = new double[3];
		for (int colour = 0; colour < 3; colour++)
			weight[colour] = max == 0 ? 0 : deficit[colour] / (double) max;

		return weight;
	}

	private static boolean known(PotionType[] orders, int[] subset)
	{
		for (int slot : subset)
		{
			if (orders[slot] == null)
				return false;
		}

		return true;
	}

	/** Lists every unordered set of three orders once, with its chance of being drawn. */
	private static List<Draw> draws()
	{
		PotionType[] types = PotionType.TYPES;
		double totalWeight = 0;
		for (PotionType type : types)
			totalWeight += type.orderWeight();

		List<Draw> draws = new ArrayList<>();
		for (int a = 0; a < types.length; a++)
		{
			for (int b = a; b < types.length; b++)
			{
				for (int c = b; c < types.length; c++)
				{
					int orderings = a == b && b == c ? 1 : (a == b || b == c ? 3 : 6);
					double chance = orderings * types[a].orderWeight() * types[b].orderWeight() * types[c].orderWeight()
							/ (totalWeight * totalWeight * totalWeight);
					draws.add(new Draw(new PotionType[]{types[a], types[b], types[c]}, chance));
				}
			}
		}

		return draws;
	}

	/** One possible set of three orders and its chance of being drawn. */
	private static final class Draw
	{
		final PotionType[] orders;
		final double chance;

		Draw(PotionType[] orders, double chance)
		{
			this.orders = orders;
			this.chance = chance;
		}
	}
}
