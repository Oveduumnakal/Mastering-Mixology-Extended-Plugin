# Recommendation strategies

The "Highlight recommended potions" option colours each order green (brew) or red (skip).
Delivering any potion refreshes all three orders, so skipping an order means brewing only the green ones.
The **Strategy** setting picks the rule set:

| Strategy | What it does | Favours |
|---|---|---|
| **Expected value** (default) | Scores every way to deliver from the 3 orders and picks the best long-run rate of useful resin per potion | The colours furthest behind, full batches when they pay, Mixalot |
| **Bottleneck** | Brew all 3 if at least 2 orders give the colour furthest behind; otherwise only those orders; otherwise the single best order | The single colour furthest behind |
| **Adaptive** | The original three-regime meta-policy described below | Depends on the deficit shape |
| **Greedy** | Always brew all 3 | XP per hour and resin per trip; ignores balance |

All strategies use the summed cost of the rewards ticked in Reward Tracking as the target.

## Expected value

1. **Candidates.** Every non-empty subset of the three orders (7 options) is a possible delivery.
2. **Useful resin.** A delivery's resin is the sum of its orders' resin times 1, 1.2 or 1.4 for one, two or three
   orders. Each colour is capped at what is still needed, so resin past the target counts for nothing.
3. **Colour weights.** Each colour's useful resin is multiplied by its deficit divided by the largest deficit.
   Every colour is offered equally often by the order draw, so the colour furthest behind takes longest to finish and
   its resin is worth the most. Without this weight the policy overshoots aga by about 1,600 resin and needs 6 % more
   potions than with it.
4. **Opportunity cost.** Each potion brewed now uses a turn that a fresh order set could have used. The plugin works
   out the long-run weighted useful resin per potion, `rate`, by Dinkelbach iteration over all 220 possible order
   sets, weighted by the in-game order chances (single-colour 5, two-colour 4, Mixalot 3).
5. **Pick.** The delivery with the highest `weighted useful resin - rate x potions` wins.

The weight exponent was swept (0, 0.5, 1, 1.5, 2, 3, 4, 8, 16). The optimum is flat between 1 and 1.5; 1 is used.

## Simulated results: Reagent Pouch

Own Monte-Carlo simulation (1,000 trials per strategy, the same order sequences for every strategy), target
13,800 mox / 11,200 aga / 15,100 lye, all 10 recipes unlocked. Model: the three orders are drawn independently by
the wiki weights, and every delivery refreshes all three.

From 0 resin:

| Strategy | Potions (mean) | p10 to p90 | vs Expected value | Deliveries | Herblore XP | Overshoot m / a / l |
|---|---:|---:|---:|---:|---:|---:|
| Expected value | 973 | 959 to 987 | | 518 | 272k | 16 / 49 / 15 |
| Adaptive | 1,010 | 993 to 1,029 | +3.8 % | 469 | 268k | 38 / 405 / 37 |
| Bottleneck | 1,013 | 996 to 1,029 | +4.1 % | 458 | 267k | 41 / 341 / 38 |
| Greedy | 1,134 | 1,092 to 1,179 | +16.5 % | 378 | 286k | 1,309 / 3,911 / 31 |

From 100 / 666 / 182 resin: Expected value 954, Adaptive 1,000, Bottleneck 1,000, Greedy 1,120.

Caveats: potions are not time. Expected value makes about 10 % more deliveries than Bottleneck (more single-potion
rerolls), and each delivery adds a walk to the conveyor. The model does not include station times, digweed, or
depositing an unordered potion to reroll.

# Adaptive meta-strategy

## The adaptive meta-strategy in one paragraph

Each turn the plugin classifies the *shape* of your remaining resin deficit
into one of three regimes and applies the sub-policy that is empirically
best for that shape:

* **single-bottleneck** (one colour clearly behind): brew slots that give
  the most-behind colour. If ≥ 2 of the 3 orders give that colour, brew
  all 3 to bank the +40 % bonus.
* **dual-bottleneck** (the top-2 deficit colours are within 20 % of each
  other): only commit to all-3 when ≥ 2 of the orders give *both* of the
  top-2 colours (strict conjunction); otherwise fall back to single-
  bottleneck behaviour.
* **balanced** (all three deficits within 10 % of each other): only brew
  multi-resin potions (no MMM / AAA / LLL); otherwise reroll the best
  single slot.

Transitions use 5 % hysteresis to avoid thrashing
(enter dual at 20 %, leave at 25 %; enter balanced at 10 %, leave at 15 %).
The state is maintained across orders and resets on plugin start-up.

## Why this policy — research notes

The recommendation was derived from a Monte-Carlo simulator that evaluates
order-submission policies across many fixed order sequences. Headline
findings (1 000 trials per policy, all-8-non-pack-reward target of
61 050 mox / 52 550 aga / 70 500 lye):

| Strategy | Mean potions brewed | vs greedy |
|---|---:|---:|
| Adaptive meta (recommended) | 4 612 | −12.8 % |
| Best static (`two_plus_bn`) | 4 606 | −12.9 % |
| Greedy ("brew all 3 every turn") | 5 287 | — |

Caveat: at the standard target shape the meta and the static policy are
**statistically tied** (top-10 variants all within ~20 potions of each
other, p10-p90 spread ~70). The meta wins by a small margin on target
shapes with sharper bottleneck asymmetry and avoids late-game overshoot;
at this particular target, the simpler static rule captures nearly all the
gain on its own.

* The simulator, the supporting policy library (R, ~1 000 trials per
  policy, 20+ candidate policies, a threshold sweep for the meta-policy),
  and the full write-up live in a companion repo:
  [PDBoegel/mastering-mixology-sim](https://github.com/PDBoegel/mastering-mixology-sim).
  See in particular
  [`STRATEGY.md`](https://github.com/PDBoegel/mastering-mixology-sim/blob/main/STRATEGY.md)
  for the methodology, threshold sweep, and verification.
* The static-policy floor was independently verified by a beam search
  exploring up to 200 000 candidate paths per turn on 5 random sequences —
  the beam never beat the policy, which is strong evidence that the meta's
  ~7 502-potion result is near the true minimum on this target.
* The recommended thresholds (20 % / 25 % / 10 % / 15 %) sit in the middle
  of a broad optimum: any sane choice in
  `t_dual_in ∈ [20 %, 50 %], t_balanced_in ∈ [10 %, 20 %], hysteresis ∈
  [2 %, 10 %]` gives essentially the same result. Outside that region
  (e.g. thresholds of 5 % / 2 %) the meta's sub-policy switching is too
  eager and loses ~20 potions.

The highlight uses the *summed cost* of every reward you've ticked in the
Reward Tracking section as the target; turn it off (or untick everything)
to disable. The decision is recomputed on every order rebuild, so toggling
a reward updates the colours immediately.
