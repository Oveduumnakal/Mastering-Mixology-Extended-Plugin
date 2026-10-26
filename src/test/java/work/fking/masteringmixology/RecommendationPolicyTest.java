package work.fking.masteringmixology;

import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static work.fking.masteringmixology.PotionType.ALCO_AUGMENTATOR;
import static work.fking.masteringmixology.PotionType.LIPLACK_LIQUOR;
import static work.fking.masteringmixology.PotionType.MAMMOTH_MIGHT_MIX;
import static work.fking.masteringmixology.PotionType.MEGALITE_LIQUID;
import static work.fking.masteringmixology.PotionType.MIXALOT;
import static work.fking.masteringmixology.PotionType.MYSTIC_MANA_AMALGAM;

/** Tests for the recommendation strategies and the potion data they read. */
public class RecommendationPolicyTest
{
	private static final int[] NO_RESIN = {0, 0, 0};

	@Test
	public void resinYieldFollowsTheRecipeShape()
	{
		assertArrayEquals(new int[]{20, 0, 0}, yields(MAMMOTH_MIGHT_MIX));
		assertArrayEquals(new int[]{20, 10, 0}, yields(MYSTIC_MANA_AMALGAM));
		assertArrayEquals(new int[]{10, 0, 20}, yields(MEGALITE_LIQUID));
		assertArrayEquals(new int[]{20, 20, 20}, yields(MIXALOT));
	}

	@Test
	public void colourCountIsTheNumberOfDifferentPastes()
	{
		assertEquals(1, MAMMOTH_MIGHT_MIX.colourCount());
		assertEquals(2, MYSTIC_MANA_AMALGAM.colourCount());
		assertEquals(3, MIXALOT.colourCount());
	}

	@Test
	public void orderWeightsMatchTheWiki()
	{
		assertEquals(5, LIPLACK_LIQUOR.orderWeight());
		assertEquals(4, MEGALITE_LIQUID.orderWeight());
		assertEquals(3, MIXALOT.orderWeight());
		int total = 0;
		for (PotionType type : PotionType.TYPES)
			total += type.orderWeight();

		assertEquals(42, total);
	}

	@Test
	public void greedyAlwaysBrewsAllThree()
	{
		var policy = RecommendationStrategy.GREEDY.createPolicy();
		var orders = new PotionType[]{MAMMOTH_MIGHT_MIX, ALCO_AUGMENTATOR, LIPLACK_LIQUOR};
		assertEquals(Set.of(0, 1, 2), policy.decide(orders, NO_RESIN, new int[]{0, 0, 100}));
	}

	@Test
	public void bottleneckBrewsAllThreeWhenTwoOrdersGiveTheTopColour()
	{
		var orders = new PotionType[]{LIPLACK_LIQUOR, MEGALITE_LIQUID, MAMMOTH_MIGHT_MIX};
		assertEquals(Set.of(0, 1, 2), new BottleneckPolicy().decide(orders, NO_RESIN, new int[]{100, 100, 1000}));
	}

	@Test
	public void bottleneckBrewsOnlyTheOrderGivingTheTopColour()
	{
		var orders = new PotionType[]{MAMMOTH_MIGHT_MIX, LIPLACK_LIQUOR, ALCO_AUGMENTATOR};
		assertEquals(Set.of(1), new BottleneckPolicy().decide(orders, NO_RESIN, new int[]{100, 100, 1000}));
	}

	@Test
	public void expectedValueBrewsThreeMixalots()
	{
		var orders = new PotionType[]{MIXALOT, MIXALOT, MIXALOT};
		var target = new int[]{5000, 5000, 5000};
		assertEquals(Set.of(0, 1, 2), new ExpectedValuePolicy().decide(orders, NO_RESIN, target));
	}

	@Test
	public void expectedValueSkipsOrdersThatGiveNothingNeeded()
	{
		var orders = new PotionType[]{MAMMOTH_MIGHT_MIX, LIPLACK_LIQUOR, ALCO_AUGMENTATOR};
		var resin = new int[]{1000, 1000, 0};
		var target = new int[]{1000, 1000, 1000};
		assertEquals(Set.of(1), new ExpectedValuePolicy().decide(orders, resin, target));
	}

	@Test
	public void expectedValueIgnoresUnknownSlots()
	{
		var orders = new PotionType[]{null, LIPLACK_LIQUOR, null};
		assertEquals(Set.of(1), new ExpectedValuePolicy().decide(orders, NO_RESIN, new int[]{1000, 1000, 1000}));
	}

	@Test
	public void expectedValueBrewsAllThreeOnceTheTargetIsMet()
	{
		var orders = new PotionType[]{MAMMOTH_MIGHT_MIX, LIPLACK_LIQUOR, ALCO_AUGMENTATOR};
		var resin = new int[]{1000, 1000, 1000};
		assertEquals(Set.of(0, 1, 2), new ExpectedValuePolicy().decide(orders, resin, resin));
	}

	@Test
	public void gainAppliesTheBatchBonusAndCapsEachColour()
	{
		var orders = new PotionType[]{MIXALOT, MIXALOT, MIXALOT};
		var weight = new double[]{1, 1, 1};
		assertEquals(60.0, ExpectedValuePolicy.gain(orders, new int[]{0}, new int[]{999, 999, 999}, weight), 1e-9);
		assertEquals(252.0, ExpectedValuePolicy.gain(orders, new int[]{0, 1, 2}, new int[]{999, 999, 999}, weight),
				1e-9);
		assertEquals(184.0, ExpectedValuePolicy.gain(orders, new int[]{0, 1, 2}, new int[]{999, 999, 16}, weight),
				1e-9);
	}

	@Test
	public void longRunRateIsWithinTheBestPossiblePerPotionYield()
	{
		int[] deficit = {1000, 1000, 1000};
		double rate = ExpectedValuePolicy.longRunRate(deficit, ExpectedValuePolicy.weights(deficit));
		assertTrue(rate > 20 && rate <= 84);
	}

	private static int[] yields(PotionType type)
	{
		return new int[]{type.resinYield(0), type.resinYield(1), type.resinYield(2)};
	}
}
