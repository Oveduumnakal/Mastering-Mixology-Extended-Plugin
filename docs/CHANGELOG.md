# Changelog

Releases from 2.0 on use their own version numbers, `Release X.Y`. Earlier entries tagged `V<upstream>-extX.Y.Z` are
Extended-fork releases; the other entries are inherited from upstream.

## 2.0
* **Recommendation strategy** setting (top of the plugin settings). Choose how orders are coloured brew or
  skip: *Expected value* (new, default), *Bottleneck*, *Adaptive* (the previous policy) or *Greedy* (brew all 3).
  Expected value needs about 4 % fewer potions than the previous policy for the Reagent Pouch in simulation.
* **Cap Concentrate clicks** now blocks only on the last tick before the Retort finishes. It used to block once a
  click would finish the potion, which cut off clicks that still saved a tick.
* **Retort quick-action colour**: while Concentrate clicks still help, the Retort is drawn in the quick-action colour,
  like the Agitator and Alembic; it returns to the station colour on the last tick.
* **Reward tracking**: the potion packs, Aldarium and the Chugging Barrel use a single quantity setting, where 0 means
  not tracked, instead of a tick box plus a quantity. Saved settings carry over.
* **Hide orders panel** option: replaces the game's potion orders list with a plugin overlay.
* **Mixing guidance** (new, all toggles in the Highlights and Misclick protection sections). Walks you through the
  mixing half of the loop: levers, then vessel, then stations, then conveyor.
  * *Highlight next lever only* — highlights just the lever for the next paste instead of all three.
  * *Lever click protection* — only the next lever can be pulled. If the vessel holds pastes that match no order,
    every lever stays pullable so you cannot get stuck. A fast double click cannot let a second paste through.
  * *Potion protection* — blocks mixing at the vessel when its three pastes do not make a potion for an open order.
  * *Mixing vessel and conveyor belt highlights* — green when they are the next thing to click.
  * *Wait for full order* — no station highlight until every order you are making has been mixed.
  * Skipped orders draw their recipe letters or blocks in the skip colour.

## V1.9.1-ext1.1.0
* **Misclick protection** (new config section, all toggles off by
  default). Contributed by @Mitchole.
  * *Lock idle stations* — per-station toggles for Alembic, Agitator,
    and Retort. Consumes the operate click when the station is empty
    and no unfulfilled order needs it with the potion you're currently
    holding. Examine and other options are untouched.
  * *Cap Concentrate clicks* — stops Retort clicks on the last tick,
    when passive fill completes the potion anyway, so spare clicks can't
    load the next potion.

## V1.9.1-ext1.0
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

## V1.9.1
* Fixed the `Fix Alembic quick-action sound effect` feature to work with the latest RuneLite version

## V1.9.0
* Added an option to fix the quick-action sound delay on the alembic. This option is enabled by default.
* Internal housekeeping to make the plugin more bulletproof against game updates.

## V1.8.1
* Fix an issue where the plugin would break the mixology overlay when boosting for better potions 

## V1.8.0
* New goal tracker feature, use it to track your progress towards the different minigame rewards
* Lowered the potion inventory overlay to make it easier to distinguish finished potions in the inventory

## V1.7.0
* Fixes stations other than the first in a batch being highlighted even with the config toggled off
* Fixes the vanilla UI enhancement for players that are boosting their herblore levels
* Adds colored inventory tags
* Improves how the `by station` potion order works to prioritize concentrate last to avoid missclicks
* Adds a `shortest past` potion order option

## V1.6.0
* Adds support for potion order reordering in the interface. For now, the only available options are: Vanilla (default) & By Station type
* Customizable notifications for digweed spawns

## V1.5.0
* Removal of the potion highlighting strategies, these were conceived when you could only deliver one potion at a time, now it is almost always better to fulfill all orders before delivering.
* Improved quick-action highlighting behavior.
* An option to display the amount of resin you have has been added, it is set to disabled by default.

## V1.4.1
* Fix repeated potion orders not being marked as fulfilled
* Fix `ready!` indicator when using the `NONE` strategy
* Replaced the `ready!` indicator with `done!`

## V1.4.0
* Separated quick-action from station highlighting
* Station highlighting is now based on potions in the inventory instead of what you're currently mixing, this should greatly help those who mix multiple potions at the same time
* Added a `ready!` indicator whenever you finish refining a potion that fulfills an order
* Added inventory potion identification similar to the Item Identification plugin, this can be disabled in the plugin settings 

## V1.3.0
* Added lever highlighting
* Fixed potion experience values

## V1.2.0
* Fixed a bug where using another station rather than the highlighted would not dismiss it even after delivering the potions
* Fixed a bug where disabling station highlights would still keep them highlighted until fulfilling an order
* Added a strategy to favor the retort station
* Added the option of having no strategy at all, thus not highlighting any potion order
* Changed when stations are highlighted, now the plugin tries to highlight them as soon as you mix a potion order correctly

## V1.1.0
* Added different configurable potion order strategy highlighting including: Favor experience, Favor alembic station, Favor mox/aga/lye
* Added station highlighting when you pick up a potion
* Added station quick-action highlighting
* Added digweed notifications & highlights