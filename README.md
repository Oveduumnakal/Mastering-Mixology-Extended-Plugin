# Mastering Mixology Extended

Fork of [Hexagon's Mastering Mixology plugin](https://github.com/hex-agon/mastering-mixology). All baseline credit goes to the original author; this fork adds multi-reward tracking, paste-quantity goals, Chugging Barrel quantity, an adaptive meta-strategy highlighter, and a full accessibility section (dyslexic / colorblind mode with custom colours for blocks, levers, and progress bars).

### Features
* Appends the potion recipe to the standard minigame interface
* Configurable potion order highlighting strategies
* Station highlighting
* Station quick-action highlighting
* Digweed highlighting & notifications
* Multi-reward tracking with overlay progress bars and a configurable
  notification when your resin pool can afford every selected reward
* **Adaptive meta-strategy highlight** — colours each order green (brew) or
  red (skip) according to a state-machine policy proven near-optimal by
  Monte-Carlo simulation (see below)
* Optional misclick protection: lock idle stations and cap Concentrate spam-clicks

### The adaptive meta-strategy in one paragraph

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

### Why this policy — research notes

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

### Changelog

Entries tagged `V<upstream>-extX.Y.Z` are Extended-fork releases; other
entries are inherited from upstream. `<upstream>` mirrors the upstream
base; `X` is the fork's major series (currently 1), `Y` bumps on new
features, `Z` bumps on ext-only fixes. The ext counters reset when
upstream ships a new version (e.g. upstream `1.9.2` → this fork
resumes at `1.9.2-ext1.0.0`).

#### V1.9.1-ext1.1.0
* **Misclick protection** (new config section, all toggles off by
  default). Contributed by @Mitchole.
  * *Lock idle stations* — per-station toggles for Alembic, Agitator,
    and Retort. Consumes the operate click when the station is empty
    and no unfulfilled order needs it with the potion you're currently
    holding. Examine and other options are untouched.
  * *Cap Concentrate clicks* — stops Retort clicks once one more would
    complete the potion; the bar coasts to completion via passive fill,
    so spare clicks can't load the next potion. Self-calibrating: adapts
    to higher Herblore levels that change per-click progress.

#### V1.9.1-ext1.0
First Extended-fork release; baseline upstream v1.9.1.

**Reward tracking**
* Multi-select goal — tick any subset of the ten rewards; overlay shows
  per-resin progress bars and a single notification when the combined
  cost is met.
* Quantity fields for every repeatable reward (packs, aldarium,
  Chugging Barrel).

**Meta-strategy**
* Recommended-Potion Highlight — colours each order green/red per an
  adaptive state-machine policy (Monte-Carlo tuned; see above). Off by
  default; needs at least one reward ticked.

**Accessibility** (new config section)
* Dyslexic / Colorblind Mode — swaps M/A/L letters for colour blocks in
  the order list and inventory tags.
* Configurable recommend/skip text colours (default gold / charcoal).
* Configurable per-resin colours (defaults `#9D4EDD` / `#00FFB3` /
  `#FF6D00`) applied to blocks, inventory tags, lever highlights, and
  progress-bar fills; live re-paint on change.

#### V1.9.1
* Fixed the `Fix Alembic quick-action sound effect` feature to work with the latest RuneLite version

#### V1.9.0
* Added an option to fix the quick-action sound delay on the alembic. This option is enabled by default.
* Internal housekeeping to make the plugin more bulletproof against game updates.

#### V1.8.1
* Fix an issue where the plugin would break the mixology overlay when boosting for better potions 

#### V1.8.0
* New goal tracker feature, use it to track your progress towards the different minigame rewards
* Lowered the potion inventory overlay to make it easier to distinguish finished potions in the inventory

#### V1.7.0
* Fixes stations other than the first in a batch being highlighted even with the config toggled off
* Fixes the vanilla UI enhancement for players that are boosting their herblore levels
* Adds colored inventory tags
* Improves how the `by station` potion order works to prioritize concentrate last to avoid missclicks
* Adds a `shortest past` potion order option

#### V1.6.0
* Adds support for potion order reordering in the interface. For now, the only available options are: Vanilla (default) & By Station type
* Customizable notifications for digweed spawns

#### V1.5.0
* Removal of the potion highlighting strategies, these were conceived when you could only deliver one potion at a time, now it is almost always better to fulfill all orders before delivering.
* Improved quick-action highlighting behavior.
* An option to display the amount of resin you have has been added, it is set to disabled by default.

#### V1.4.1
* Fix repeated potion orders not being marked as fulfilled
* Fix `ready!` indicator when using the `NONE` strategy
* Replaced the `ready!` indicator with `done!`

#### V1.4.0
* Separated quick-action from station highlighting
* Station highlighting is now based on potions in the inventory instead of what you're currently mixing, this should greatly help those who mix multiple potions at the same time
* Added a `ready!` indicator whenever you finish refining a potion that fulfills an order
* Added inventory potion identification similar to the Item Identification plugin, this can be disabled in the plugin settings 

#### V1.3.0
* Added lever highlighting
* Fixed potion experience values

#### V1.2.0
* Fixed a bug where using another station rather than the highlighted would not dismiss it even after delivering the potions
* Fixed a bug where disabling station highlights would still keep them highlighted until fulfilling an order
* Added a strategy to favor the retort station
* Added the option of having no strategy at all, thus not highlighting any potion order
* Changed when stations are highlighted, now the plugin tries to highlight them as soon as you mix a potion order correctly

#### V1.1.0
* Added different configurable potion order strategy highlighting including: Favor experience, Favor alembic station, Favor mox/aga/lye
* Added station highlighting when you pick up a potion
* Added station quick-action highlighting
* Added digweed notifications & highlights