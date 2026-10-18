# Adaptive meta-strategy

The "Highlight recommended potions" option colours each order green (brew) or red (skip).
This page explains how it decides and the research behind it.

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
