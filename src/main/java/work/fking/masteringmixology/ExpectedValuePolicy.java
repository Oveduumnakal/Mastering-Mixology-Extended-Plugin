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
 * for the target. Each colour's resin is then weighted by the cube of its deficit relative to the largest one: the
 * colour furthest behind takes longest to finish, so its resin is worth the most, and the steep weighting keeps the
 * other colours from finishing early and wasting resin. The exponent 3 was the fastest of 0.5 to 10 in simulation.
 * Delivering refreshes all three orders, so each potion brewed now has an opportunity cost: the
 * useful resin per potion the player can expect from future order sets. That rate is found by Dinkelbach iteration
 * over every possible order set, weighted by the in-game order chances, and the delivery that beats it by the most
 * wins.
 *
 * <p>Time is counted in average potions. Each delivery also costs {@link #DEPOSIT_COST} potions of time for the walk
 * to the conveyor belt and the start of the next set. Every choice includes exactly one delivery, so this cost does
 * not change the comparison within one order set; it lowers the long-run rate, which makes extra potions in the
 * current set worth brewing more often. When the orders' modifiers are known, each potion in the current set costs its
 * station's time (see {@link #potionCost(PotionModifier)}); future sets use the average.
 */
public class ExpectedValuePolicy implements RecommendationPolicy
{
	private static final int[][] SUBSETS = {{0}, {1}, {2}, {0, 1}, {0, 2}, {1, 2}, {0, 1, 2}};
	private static final int MAX_ITERATIONS = 50;
	private static final double EPSILON = 1e-6;

	/**
	 * The time one delivery costs, in average potions. A fit over 40 order sets of 2026-10-29 lab play gave 11.3
	 * seconds per delivery, and 15.5 (Alembic), 18.7 (Retort) and 15.4 (Agitator) seconds per potion.
	 */
	static final double DEPOSIT_COST = 0.7;

	/** The power each colour's relative deficit is raised to for its weight. */
	static final double WEIGHT_EXPONENT = 3;
	private static final double[] AVERAGE_COST = {1, 1, 1};
	private static final List<Draw> DRAWS = draws();

	@Override
	public Set<Integer> decide(PotionType[] orders, int[] resin, int[] target)
	{
		return decide(orders, new PotionModifier[orders.length], resin, target);
	}

	@Override
	public Set<Integer> decide(PotionType[] orders, PotionModifier[] modifiers, int[] resin, int[] target)
	{
		int[] deficit = MetaPolicy.deficit(resin, target);
		if (deficit[0] == 0 && deficit[1] == 0 && deficit[2] == 0)
			return MetaPolicy.all3();

		double[] cost = new double[orders.length];
		for (int slot = 0; slot < orders.length; slot++)
			cost[slot] = potionCost(modifiers[slot]);

		double[] weight = weights(deficit);
		int[] best = bestSubset(orders, cost, deficit, weight, longRunRate(deficit, weight));
		Set<Integer> slots = new HashSet<>();
		if (best != null)
		{
			for (int slot : best)
				slots.add(slot);
		}

		return slots;
	}

	/**
	 * Returns the weighted useful resin per potion of time that the best play earns on average from a fresh order
	 * set, holding the deficit fixed. Each delivery adds {@link #DEPOSIT_COST} to the time.
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
				int[] best = bestSubset(draw.orders, AVERAGE_COST, deficit, weight, rate);
				resin += draw.chance * gain(draw.orders, best, deficit, weight);
				potions += draw.chance * (best.length + DEPOSIT_COST);
			}

			double next = resin / potions;
			if (Math.abs(next - rate) < EPSILON)
				return next;

			rate = next;
		}

		return rate;
	}

	/**
	 * Returns the time one potion takes at its modifier's station, in average potions; 1 when the modifier is not
	 * known.
	 */
	static double potionCost(PotionModifier modifier)
	{
		if (modifier == null)
			return 1;

		switch (modifier)
		{
			case CRYSTALISED:
				return 0.94;
			case CONCENTRATED:
				return 1.13;
			case HOMOGENOUS:
				return 0.93;
			default:
				return 1;
		}
	}

	/**
	 * Returns the delivery with the highest weighted useful resin minus the opportunity cost of its potions' time; a
	 * tie goes to the delivery with more useful resin. Returns null when no order is known.
	 *
	 * @param cost each slot's potion time, in average potions
	 */
	static int[] bestSubset(PotionType[] orders, double[] cost, int[] deficit, double[] weight, double rate)
	{
		int[] best = null;
		double bestScore = Double.NEGATIVE_INFINITY;
		double bestGain = -1;
		for (int[] subset : SUBSETS)
		{
			if (!known(orders, subset))
				continue;

			double gain = gain(orders, subset, deficit, weight);
			double time = 0;
			for (int slot : subset)
				time += cost[slot];

			double score = gain - rate * time;
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

	/**
	 * Weights each colour by its deficit relative to the largest one, raised to {@link #WEIGHT_EXPONENT}, from 0 (not
	 * needed) to 1.
	 */
	static double[] weights(int[] deficit)
	{
		int max = Math.max(deficit[0], Math.max(deficit[1], deficit[2]));
		double[] weight = new double[3];
		for (int colour = 0; colour < 3; colour++)
			weight[colour] = max == 0 ? 0 : Math.pow(deficit[colour] / (double) max, WEIGHT_EXPONENT);

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
