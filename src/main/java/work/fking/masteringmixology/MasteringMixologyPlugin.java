package work.fking.masteringmixology;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.inject.Inject;

import com.google.inject.Provides;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.api.Client;
import net.runelite.api.FontID;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.Player;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.SoundEffectPlayed;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetTextAlignment;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;

import static work.fking.masteringmixology.AlchemyObject.AGA_LEVER;
import static work.fking.masteringmixology.AlchemyObject.LYE_LEVER;
import static work.fking.masteringmixology.AlchemyObject.MOX_LEVER;
import static work.fking.masteringmixology.PotionComponent.AGA;
import static work.fking.masteringmixology.PotionComponent.LYE;
import static work.fking.masteringmixology.PotionComponent.MOX;

/**
 * RuneLite plugin that extends the Mastering Mixology minigame: it adds each order's recipe to the interface and
 * highlights the stations, levers, vessel and quick actions to use next. It also tracks reward goals, recommends
 * orders with a selectable strategy, protects against misclicks and offers accessibility colours.
 */
@PluginDescriptor(name = "Mastering Mixology Extended")
public class MasteringMixologyPlugin extends Plugin
{
	private static final Logger LOGGER = LoggerFactory.getLogger(MasteringMixologyPlugin.class);

	private static final int PROC_MASTERING_MIXOLOGY_BUILD_POTION_ORDERS = 7063;
	private static final int PROC_MASTERING_MIXOLOGY_BUILD_REAGENTS = 7064;

	private static final int VARBIT_POTION_ORDER_1 = VarbitID.MM_LAB_ORDER_1_TYPE;
	private static final int VARBIT_POTION_MODIFIER_1 = VarbitID.MM_LAB_ORDER_1_MODIFIER;
	private static final int VARBIT_POTION_ORDER_2 = VarbitID.MM_LAB_ORDER_2_TYPE;
	private static final int VARBIT_POTION_MODIFIER_2 = VarbitID.MM_LAB_ORDER_2_MODIFIER;
	private static final int VARBIT_POTION_ORDER_3 = VarbitID.MM_LAB_ORDER_3_TYPE;
	private static final int VARBIT_POTION_MODIFIER_3 = VarbitID.MM_LAB_ORDER_3_MODIFIER;

	static final int VARP_LYE_RESIN = VarPlayerID.MIXOLOGY_LYE_POINTS;
	static final int VARP_AGA_RESIN = VarPlayerID.MIXOLOGY_AGA_POINTS;
	static final int VARP_MOX_RESIN = VarPlayerID.MIXOLOGY_MOX_POINTS;

	private static final int VARBIT_ALEMBIC_PROGRESS = VarbitID.MM_ALEMBIC_PROGRESS;
	private static final int VARBIT_AGITATOR_PROGRESS = VarbitID.MM_AGITATOR_PROGRESS;

	private static final int VARBIT_AGITATOR_QUICKACTION = VarbitID.MM_LAB_HIT_SKILLSHOT_AGITATOR;
	private static final int VARBIT_ALEMBIC_QUICKACTION = VarbitID.MM_LAB_HIT_SKILLSHOT_ALEMBIC;

	private static final int VARBIT_MIXING_VESSEL_POTION = VarbitID.MM_LAB_VESSEL_READY;
	private static final int VARBIT_AGITATOR_POTION = VarbitID.MM_LAB_AGITATOR_POTION;
	private static final int VARBIT_RETORT_POTION = VarbitID.MM_LAB_RETORT_POTION;
	private static final int VARBIT_ALEMBIC_POTION = VarbitID.MM_LAB_ALEMBIC_POTION;

	private static final int VARBIT_MIXER_SLOT_0 = VarbitID.MM_LAB_MIXER_SLOT_0;
	private static final int VARBIT_MIXER_SLOT_1 = VarbitID.MM_LAB_MIXER_SLOT_1;
	private static final int VARBIT_MIXER_SLOT_2 = VarbitID.MM_LAB_MIXER_SLOT_2;

	private static final Color READY_HIGHLIGHT_COLOR = Color.GREEN;

	/**
	 * Game ticks a let-through lever pull is counted as in the vessel while waiting for the mixer varbits.
	 */
	private static final int PENDING_LEVER_PULL_TICKS = 4;

	/**
	 * Index into {@link #resinIconIdx} of the block sprite drawn in the skip colour.
	 */
	private static final int SKIP_ICON = 3;

	private static final int VARBIT_DIGWEED_NORTH_EAST = VarbitID.MM_HERB_READY_1;
	private static final int VARBIT_DIGWEED_SOUTH_EAST = VarbitID.MM_HERB_READY_2;
	private static final int VARBIT_DIGWEED_SOUTH_WEST = VarbitID.MM_HERB_READY_3;
	private static final int VARBIT_DIGWEED_NORTH_WEST = VarbitID.MM_HERB_READY_4;

	private static final int SPOT_ANIM_AGITATOR = SpotanimID.VFX_MACHINERY_ALCHEMY01_AGITATOR01;
	private static final int SPOT_ANIM_ALEMBIC = SpotanimID.VFX_MACHINERY_ALCHEMY01_ALEMBIC01;

	private static final int COMPONENT_POTION_ORDERS_GROUP_ID = InterfaceID.MM_OVERLAY;
	private static final int COMPONENT_POTION_ORDERS = InterfaceID.MmOverlay.CONTENT;

	private static final int LABS_REGION_ID = 5521;
	private static final int LABS_REGION_PLANE = 0;

	private static final int FOUND_GEM = 2655;

	@Inject
	private Client client;

	@Inject
	private MasteringMixologyConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private Notifier notifier;

	@Inject
	private ClientThread clientThread;

	@Inject
	private MasteringMixologyOverlay overlay;

	@Inject
	private InventoryPotionOverlay potionOverlay;

	@Inject
	private OrdersPanelOverlay ordersPanelOverlay;

	@Inject
	private GoalInfoBoxOverlay goalInfoBoxOverlay;

	private final Map<AlchemyObject, HighlightedObject> highlightedObjects = new LinkedHashMap<>();
	private List<PotionOrder> potionOrders = Collections.emptyList();
	private boolean inLab = false;

	private PotionType alembicPotionType;
	private PotionType agitatorPotionType;
	private PotionType retortPotionType;

	private int previousAgitatorProgess;
	private int previousAlembicProgress;

	private final RetortProgressTracker retortProgressTracker = new RetortProgressTracker();

	private int agitatorQuickActionTicks = 0;
	private int alembicQuickActionTicks = 0;

	private boolean previouslyAboveRewardThreshold = false;

	private final Goal goal = new Goal(RewardItem.NONE);

	/** The policy behind the recommended-potion highlight, created from the Strategy setting. */
	private RecommendationPolicy recommendationPolicy = RecommendationStrategy.EXPECTED_VALUE.createPolicy();

	/**
	 * Order slots the recommendation policy picked on the last order rebuild; null when recommendations are off.
	 */
	private Set<Integer> recommendedSlots = null;
	/** The sprite the game shows beside each order slot, which is the icon of the station its modifier needs. */
	private final Map<Integer, Integer> orderSprites = new HashMap<>();

	/**
	 * Lever state used by lever click protection; counts pending lever pulls as already in the vessel.
	 */
	private MixGuidance.LeverState leverState = MixGuidance.LeverState.FREE;

	/**
	 * Lever state used for highlights; follows the confirmed vessel contents, so a pulled lever stays lit
	 * until its paste lands.
	 */
	private MixGuidance.LeverState highlightLeverState = MixGuidance.LeverState.FREE;

	/**
	 * True once the mixed potion has reached the inventory but the game has not yet cleared the vessel varbits.
	 * While set, the vessel is treated as empty so its highlight and the next lever update without that lag.
	 */
	private boolean vesselCollected = false;

	/**
	 * Lever pulls the client has sent that the mixer varbits do not show yet, oldest first. They count as already
	 * in the vessel, so a fast double click cannot add a second paste before the server catches up.
	 */
	private final List<PotionComponent> pendingLeverPulls = new ArrayList<>();

	private int pendingLeverPullsExpireTick = 0;

	private int unfinishedInInventory = 0;

	private int[] resinIconIdx = null;
	private net.runelite.api.IndexedSprite[] resinIconSprites = null;

	public Map<AlchemyObject, HighlightedObject> highlightedObjects()
	{
		return highlightedObjects;
	}

	public boolean isInLab()
	{
		return inLab;
	}

	/** Returns the current orders, in the order they are displayed. */
	List<PotionOrder> potionOrders()
	{
		return potionOrders;
	}

	/** Returns the order slots the recommendation policy picks, or null when recommendations are off. */
	Set<Integer> recommendedSlots()
	{
		return recommendedSlots;
	}

	/** Returns the sprite id of the station icon beside the order in the given slot, or -1 before it has been read. */
	int orderSpriteId(int idx)
	{
		return orderSprites.getOrDefault(idx, -1);
	}

	/** Returns the paste of the given colour left in the hopper. */
	int pasteAmount(PotionComponent component)
	{
		switch (component)
		{
			case MOX: return client.getVarbitValue(VarbitID.MM_AVAILABLE_MOX);
			case AGA: return client.getVarbitValue(VarbitID.MM_AVAILABLE_AGA);
			default:  return client.getVarbitValue(VarbitID.MM_AVAILABLE_LYE);
		}
	}

	/** Returns the resin of the given colour the player has earned. */
	int resinAmount(PotionComponent component)
	{
		return client.getVarpValue(component.resinVarpId());
	}

	/**
	 * @return true if the player is in the labs region (the area where the minigame takes place)
	 * the isInlab method only checks if they are inside the actual lab room where the UI is active
	 */
	public boolean isInLabRegion()
	{
		Player player = client.getLocalPlayer();
		return player != null && player.getWorldLocation().getRegionID() == LABS_REGION_ID
				&& player.getWorldLocation().getPlane() == LABS_REGION_PLANE;
	}

	@Provides
	MasteringMixologyConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MasteringMixologyConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		overlayManager.add(potionOverlay);
		overlayManager.add(goalInfoBoxOverlay);
		overlayManager.add(ordersPanelOverlay);
		previouslyAboveRewardThreshold = false;
		recommendationPolicy = config.recommendationStrategy().createPolicy();
		clientThread.invokeLater(this::registerResinIcons);

		if (client.getGameState() == GameState.LOGGED_IN)
			clientThread.invokeLater(this::initialize);
	}

	private void registerResinIcons()
	{
		if (resinIconIdx != null)
		{
			applyResinIconPalettes();
			return;
		}

		net.runelite.api.IndexedSprite[] mod = client.getModIcons();
		if (mod == null)
			return;

		final int count = SKIP_ICON + 1;
		int base = mod.length;
		net.runelite.api.IndexedSprite[] extended = Arrays.copyOf(mod, base + count);
		int[] indices = new int[count];
		net.runelite.api.IndexedSprite[] sprites = new net.runelite.api.IndexedSprite[count];
		for (int i = 0; i < count; i++)
		{
			net.runelite.api.IndexedSprite s = createBlockSprite(resinIconColor(i));
			extended[base + i] = s;
			indices[i] = base + i;
			sprites[i] = s;
		}

		client.setModIcons(extended);
		resinIconIdx = indices;
		resinIconSprites = sprites;
	}

	private net.runelite.api.IndexedSprite createBlockSprite(Color color)
	{
		final int size = 9;
		net.runelite.api.IndexedSprite s = client.createIndexedSprite();
		byte[] pixels = new byte[size * size];
		for (int i = 0; i < pixels.length; i++)
		{
			int x = i % size;
			int y = i / size;
			boolean edge = x == 0 || x == size - 1 || y == 0 || y == size - 1;
			pixels[i] = (byte) (edge ? 1 : 2);
		}

		s.setPixels(pixels);
		s.setPalette(blockPalette(color));
		s.setWidth(size);
		s.setHeight(size);
		s.setOriginalWidth(size);
		s.setOriginalHeight(size);
		return s;
	}

	private static int[] blockPalette(Color color)
	{
		return new int[]{0, 0x000000, color.getRGB() & 0xFFFFFF};
	}

	/**
	 * @param icon a component ordinal, or {@link #SKIP_ICON}
	 */
	private Color resinIconColor(int icon)
	{
		if (icon == SKIP_ICON)
			return config.accessibilitySkipColor();

		return effectiveComponentColor(PotionComponent.ENTRIES[icon]);
	}

	private void applyResinIconPalettes()
	{
		if (resinIconSprites == null)
			return;

		for (int i = 0; i < resinIconSprites.length; i++)
		{
			net.runelite.api.IndexedSprite s = resinIconSprites[i];
			if (s != null)
				s.setPalette(blockPalette(resinIconColor(i)));
		}
	}

	Color effectiveComponentColor(PotionComponent c)
	{
		if (!config.accessibilityMode())
			return c.color();

		switch (c)
		{
			case MOX: return config.accessibilityMoxColor();
			case AGA: return config.accessibilityAgaColor();
			case LYE: return config.accessibilityLyeColor();
			default:  return c.color();
		}
	}

	/**
	 * Removes the overlays and highlights and clears the per-session state. The game's orders panel is shown again
	 * and rebuilt, so neither a hidden panel nor the appended recipes outlive the plugin.
	 */
	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		overlayManager.remove(potionOverlay);
		overlayManager.remove(goalInfoBoxOverlay);
		overlayManager.remove(ordersPanelOverlay);
		inLab = false;
		highlightedObjects.clear();
		agitatorQuickActionTicks = 0;
		alembicQuickActionTicks = 0;
		pendingLeverPulls.clear();
		vesselCollected = false;
		retortProgressTracker.onPotionCleared();
		clientThread.invokeLater(() ->
		{
			var baseWidget = client.getWidget(COMPONENT_POTION_ORDERS);
			if (baseWidget != null)
				setOrdersPanelHidden(baseWidget, false);

			triggerPotionOrderUpdate();
		});
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING)
			highlightedObjects.clear();
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() != COMPONENT_POTION_ORDERS_GROUP_ID)
			return;

		initialize();
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed event)
	{
		if (event.getGroupId() != COMPONENT_POTION_ORDERS_GROUP_ID)
			return;

		highlightedObjects.clear();
		inLab = false;
	}

	/**
	 * Applies a settings change straight away: re-sorts the orders, refreshes the station highlights and the resin
	 * display, clears the digweed highlights when they are turned off and recalculates the goal.
	 *
	 * <p>The orders are also re-decorated when a highlight-related setting changes, so the player sees the effect
	 * immediately without waiting for the next order rebuild.
	 */
	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals(MasteringMixologyConfig.CONFIG_GROUP))
			return;

		if (event.getKey().equals("potionOrderSorting"))
			clientThread.invokeLater(this::updatePotionOrders);

		if (event.getKey().equals("highlightStations") || event.getKey().equals("waitForFullOrder"))
			clientThread.invokeLater(this::refreshStationHighlight);

		if (event.getKey().equals("displayResin"))
			clientThread.invokeLater(this::triggerPotionOrderUpdate);

		if (!config.highlightDigWeed())
		{
			unHighlightObject(AlchemyObject.DIGWEED_NORTH_EAST);
			unHighlightObject(AlchemyObject.DIGWEED_SOUTH_EAST);
			unHighlightObject(AlchemyObject.DIGWEED_SOUTH_WEST);
			unHighlightObject(AlchemyObject.DIGWEED_NORTH_WEST);
		}

		if (event.getKey().equals("showResinBars") || event.getKey().startsWith("track")
				|| event.getKey().endsWith("Quantity"))
		{
			recalculateGoalData();
			previouslyAboveRewardThreshold = false;
		}

		if (event.getKey().equals("hideOrdersPanel"))
			clientThread.invokeLater(() ->
			{
				var baseWidget = client.getWidget(COMPONENT_POTION_ORDERS);
				if (baseWidget != null)
					setOrdersPanelHidden(baseWidget, config.hideOrdersPanel());

				triggerPotionOrderUpdate();
			});

		if (event.getKey().equals("recommendationStrategy"))
			recommendationPolicy = config.recommendationStrategy().createPolicy();

		if (event.getKey().equals("highlightRecommendedPotions")
				|| event.getKey().equals("recommendationStrategy")
				|| event.getKey().equals("recommendedPotionColor")
				|| event.getKey().equals("notRecommendedPotionColor")
				|| event.getKey().equals("dyslexicMixology")
				|| event.getKey().equals("accessibilityRecommendColor")
				|| event.getKey().equals("accessibilitySkipColor"))
		{
			recommendationPolicy.reset();
			clientThread.invokeLater(this::triggerPotionOrderUpdate);
		}

		if (event.getKey().equals("dyslexicMixology")
				|| event.getKey().equals("accessibilityMoxColor")
				|| event.getKey().equals("accessibilityAgaColor")
				|| event.getKey().equals("accessibilityLyeColor")
				|| event.getKey().equals("accessibilitySkipColor"))
			clientThread.invokeLater(this::applyResinIconPalettes);

		clientThread.invokeLater(this::updateMixGuidance);
	}

	/**
	 * Keeps the unfinished-potion count and the station highlight in step with the inventory.
	 *
	 * <p>The highlight is left alone while a potion is in a station. Otherwise it goes to the station of the first
	 * potion in the inventory that still needs one.
	 */
	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (inLab && event.getContainerId() == InventoryID.INV)
		{
			int unfinished = countUnfinishedInInventory();
			if (unfinished > unfinishedInInventory
					&& MixGuidance.vesselFull(mixerSlots(), client.getVarbitValue(VARBIT_MIXING_VESSEL_POTION)))
				vesselCollected = true;

			unfinishedInInventory = unfinished;
			updateMixGuidance();
		}

		if (!inLab || !config.highlightStations() || event.getContainerId() != InventoryID.INV)
			return;

		if (alembicPotionType != null || agitatorPotionType != null || retortPotionType != null)
			return;

		if (waitingForFullOrder())
		{
			unHighlightAllStations();
			return;
		}

		var inventory = event.getItemContainer();

		for (var item : inventory.getItems())
		{
			var potionType = PotionType.fromItemId(item.getId());

			if (potionType == null || potionType.modifiedItemId() == item.getId())
				continue;

			for (var order : potionOrders)
			{
				if (order.potionType() == potionType && !order.fulfilled())
				{
					unHighlightAllStations();
					highlightObject(order.potionModifier().alchemyObject(), config.stationHighlightColor());
					return;
				}
			}
		}
	}

	/**
	 * Reacts to the lab's varbits and varps.
	 *
	 * <p>Delivering a potion resets every order varbit to 0 before setting the new values, so the first order varbit
	 * dropping to 0 clears all the stations. A station's potion varbit returning to 0 means that station has finished:
	 * its highlight is removed and the matching order is fulfilled. A progress varbit that drops means a quick action
	 * failed and a quick-action varbit means one was just popped, so both reset that station's highlight. The
	 * Agitator's quick action has a window of two ticks and the Alembic's of one, and the highlight is also reset
	 * once that window has passed. Resin varps recalculate the goal.
	 */
	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		var varbitId = event.getVarbitId();
		var varpId = event.getVarpId();
		var value = event.getValue();

		if (varbitId == VARBIT_POTION_ORDER_1)
		{
			if (value == 0)
				unHighlightAllStations();
			else
				clientThread.invokeAtTickEnd(this::updatePotionOrders);
		}
		else if (varbitId == VARBIT_ALEMBIC_POTION)
		{
			if (value == 0)
			{
				unHighlightObject(AlchemyObject.ALEMBIC);
				tryFulfillOrder(alembicPotionType, PotionModifier.CRYSTALISED);
				tryHighlightNextStation();
				LOGGER.debug("Finished crystalising {}", alembicPotionType);
				alembicPotionType = null;
			}
			else
			{
				alembicPotionType = PotionType.fromIdx(value - 1);
				LOGGER.debug("Alembic potion type: {}", alembicPotionType);
			}
		}
		else if (varbitId == VARBIT_AGITATOR_POTION)
		{
			if (value == 0)
			{
				unHighlightObject(AlchemyObject.AGITATOR);
				tryFulfillOrder(agitatorPotionType, PotionModifier.HOMOGENOUS);
				tryHighlightNextStation();
				LOGGER.debug("Finished homogenising {}", agitatorPotionType);
				agitatorPotionType = null;
			}
			else
			{
				agitatorPotionType = PotionType.fromIdx(value - 1);
				LOGGER.debug("Agitator potion type: {}", agitatorPotionType);
			}
		}
		else if (varbitId == VARBIT_RETORT_POTION)
		{
			if (value == 0)
			{
				unHighlightObject(AlchemyObject.RETORT);
				tryFulfillOrder(retortPotionType, PotionModifier.CONCENTRATED);
				tryHighlightNextStation();
				LOGGER.debug("Finished concentrating {}", retortPotionType);
				retortPotionType = null;
				retortProgressTracker.onPotionCleared();
			}
			else
			{
				retortPotionType = PotionType.fromIdx(value - 1);
				LOGGER.debug("Retort potion type: {}", retortPotionType);
				retortProgressTracker.onPotionLoaded();
			}
		}
		else if (varbitId == VARBIT_DIGWEED_NORTH_EAST)
		{
			if (value == 1)
			{
				if (config.highlightDigWeed())
					highlightObject(AlchemyObject.DIGWEED_NORTH_EAST, config.digweedHighlightColor());

				notifier.notify(config.notifyDigWeed(), "A digweed has spawned north east.");
			}
			else
			{
				unHighlightObject(AlchemyObject.DIGWEED_NORTH_EAST);
			}
		}
		else if (varbitId == VARBIT_DIGWEED_SOUTH_EAST)
		{
			if (value == 1)
			{
				if (config.highlightDigWeed())
					highlightObject(AlchemyObject.DIGWEED_SOUTH_EAST, config.digweedHighlightColor());

				notifier.notify(config.notifyDigWeed(), "A digweed has spawned south east.");
			}
			else
			{
				unHighlightObject(AlchemyObject.DIGWEED_SOUTH_EAST);
			}
		}
		else if (varbitId == VARBIT_DIGWEED_SOUTH_WEST)
		{
			if (value == 1)
			{
				if (config.highlightDigWeed())
					highlightObject(AlchemyObject.DIGWEED_SOUTH_WEST, config.digweedHighlightColor());

				notifier.notify(config.notifyDigWeed(), "A digweed has spawned south west.");
			}
			else
			{
				unHighlightObject(AlchemyObject.DIGWEED_SOUTH_WEST);
			}
		}
		else if (varbitId == VARBIT_DIGWEED_NORTH_WEST)
		{
			if (value == 1)
			{
				if (config.highlightDigWeed())
					highlightObject(AlchemyObject.DIGWEED_NORTH_WEST, config.digweedHighlightColor());

				notifier.notify(config.notifyDigWeed(), "A digweed has spawned north west.");
			}
			else
			{
				unHighlightObject(AlchemyObject.DIGWEED_NORTH_WEST);
			}
		}
		else if (varbitId == VARBIT_AGITATOR_PROGRESS)
		{
			if (agitatorQuickActionTicks == 2)
			{
				resetStationHighlight(AlchemyObject.AGITATOR);
				agitatorQuickActionTicks = 0;
			}

			if (agitatorQuickActionTicks == 1)
				agitatorQuickActionTicks = 2;

			if (value < previousAgitatorProgess)
				resetStationHighlight(AlchemyObject.AGITATOR);

			previousAgitatorProgess = value;
		}
		else if (varbitId == VARBIT_ALEMBIC_PROGRESS)
		{
			if (alembicQuickActionTicks == 1)
			{
				resetStationHighlight(AlchemyObject.ALEMBIC);
				alembicQuickActionTicks = 0;
			}

			if (value < previousAlembicProgress)
				resetStationHighlight(AlchemyObject.ALEMBIC);

			previousAlembicProgress = value;
		}
		else if (varbitId == VarbitID.MM_RETORT_PROGRESS)
		{
			retortProgressTracker.onProgressChanged(value);
			updateRetortQuickActionHighlight();
		}
		else if (varbitId == VARBIT_AGITATOR_QUICKACTION)
		{
			resetStationHighlight(AlchemyObject.AGITATOR);
		}
		else if (varbitId == VARBIT_ALEMBIC_QUICKACTION)
		{
			resetStationHighlight(AlchemyObject.ALEMBIC);
		}
		else if (varpId == VARP_MOX_RESIN || varpId == VARP_AGA_RESIN || varpId == VARP_LYE_RESIN)
		{
			recalculateGoalData();
			checkRewardThresholdNotification();
		}

		if (value != 0 && isMixerSlot(varbitId) && !pendingLeverPulls.isEmpty())
			pendingLeverPulls.remove(0);

		if (inLab && affectsMixGuidance(varbitId))
			updateMixGuidance();
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (pendingLeverPulls.isEmpty() || client.getTickCount() < pendingLeverPullsExpireTick)
			return;

		pendingLeverPulls.clear();
		updateMixGuidance();
	}

	private static boolean isMixerSlot(int varbitId)
	{
		return varbitId == VARBIT_MIXER_SLOT_0
				|| varbitId == VARBIT_MIXER_SLOT_1
				|| varbitId == VARBIT_MIXER_SLOT_2;
	}

	private static boolean affectsMixGuidance(int varbitId)
	{
		return isMixerSlot(varbitId)
				|| varbitId == VARBIT_MIXING_VESSEL_POTION
				|| varbitId == VARBIT_ALEMBIC_POTION
				|| varbitId == VARBIT_AGITATOR_POTION
				|| varbitId == VARBIT_RETORT_POTION;
	}

	/**
	 * Notifies once when the goal first becomes affordable.
	 *
	 * <p>{@link Goal} has already summed the costs, including the pack quantities, and its overall progress
	 * is the mean of each colour's clamped percentage, so it reaches 1.0 only when every colour meets its
	 * summed goal.
	 */
	private void checkRewardThresholdNotification()
	{
		if (!goal.isMultiMode())
		{
			previouslyAboveRewardThreshold = false;
			return;
		}

		boolean nowAbove = goal.getOverallProgress() >= 1.0;
		if (nowAbove && !previouslyAboveRewardThreshold)
		{
			notifier.notify(config.thresholdNotification(),
					"You have enough resin to purchase your selected Mastering Mixology rewards.");
		}

		previouslyAboveRewardThreshold = nowAbove;
	}

	@Subscribe
	public void onSoundEffectPlayed(SoundEffectPlayed event)
	{
		if (inLab && alembicPotionType != null && event.getSoundId() == FOUND_GEM && event.getDelay() > 0
				&& config.soundEffectAlembic())
		{
			LOGGER.debug("client found_gem sound effect detected during Alembic, blocking");
			event.consume();
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (!inLab)
			return;

		MenuAction action = event.getMenuAction();
		if (action != MenuAction.GAME_OBJECT_FIRST_OPTION
				&& action != MenuAction.GAME_OBJECT_SECOND_OPTION
				&& action != MenuAction.GAME_OBJECT_THIRD_OPTION
				&& action != MenuAction.GAME_OBJECT_FOURTH_OPTION
				&& action != MenuAction.GAME_OBJECT_FIFTH_OPTION)
			return;

		if (isLeverBlocked(event.getId()) || isVesselBlocked(event.getId()))
		{
			event.consume();
			return;
		}

		recordLeverPull(event.getId());
		AlchemyObject station = StationGuard.stationFromTarget(event.getMenuTarget());
		if (station == null)
			return;

		if (isCapBlocked(station) || isLockBlocked(station))
			event.consume();
	}

	private boolean isLeverBlocked(int objectId)
	{
		if (!config.leverClickProtection())
			return false;

		PotionComponent lever = leverComponent(objectId);
		if (lever == null)
			return false;

		updateMixGuidance();
		return !leverState.allows(lever);
	}

	/**
	 * Counts a let-through lever click as a paste already in the vessel until the mixer varbits confirm it.
	 */
	private void recordLeverPull(int objectId)
	{
		PotionComponent lever = leverComponent(objectId);
		if (lever == null)
			return;

		pendingLeverPulls.add(lever);
		pendingLeverPullsExpireTick = client.getTickCount() + PENDING_LEVER_PULL_TICKS;
		updateMixGuidance();
	}

	private static PotionComponent leverComponent(int objectId)
	{
		if (objectId == MOX_LEVER.objectId())
			return MOX;

		if (objectId == AGA_LEVER.objectId())
			return AGA;

		if (objectId == LYE_LEVER.objectId())
			return LYE;

		return null;
	}

	private boolean isVesselBlocked(int objectId)
	{
		if (!config.potionProtection() || objectId != AlchemyObject.MIXING_VESSEL.objectId())
			return false;

		int[] slots = mixerSlots();
		int vesselReady = client.getVarbitValue(VARBIT_MIXING_VESSEL_POTION);
		if (!MixGuidance.vesselFull(slots, vesselReady))
			return false;

		return !vesselMixValid(slots, vesselReady);
	}

	private boolean vesselMixValid(int[] slots, int vesselReady)
	{
		PotionType mix = MixGuidance.vesselPotion(slots, vesselReady);
		return MixGuidance.mixMatchesOpenOrder(mix, potionOrders, potionsOnHand(false));
	}

	private boolean isCapBlocked(AlchemyObject station)
	{
		if (station != AlchemyObject.RETORT || !config.capConcentrate() || !retortProgressTracker.potionLoaded())
			return false;

		return StationGuard.concentrateCapReached(
				retortProgressTracker.currentProgress(),
				StationGuard.RETORT_PROGRESS_MAX);
	}

	private boolean isLockBlocked(AlchemyObject station)
	{
		if (!isLockEnabled(station))
			return false;

		boolean occupied = stationOccupied(station);
		return !StationGuard.isStationNeeded(station, occupied, potionOrders, heldItemIds());
	}

	private boolean isLockEnabled(AlchemyObject station)
	{
		if (station == AlchemyObject.ALEMBIC)
			return config.lockAlembicWhenIdle();

		if (station == AlchemyObject.AGITATOR)
			return config.lockAgitatorWhenIdle();

		if (station == AlchemyObject.RETORT)
			return config.lockRetortWhenIdle();

		return false;
	}

	private boolean stationOccupied(AlchemyObject station)
	{
		if (station == AlchemyObject.ALEMBIC)
			return alembicPotionType != null;

		if (station == AlchemyObject.AGITATOR)
			return agitatorPotionType != null;

		if (station == AlchemyObject.RETORT)
			return retortPotionType != null;

		return false;
	}

	private Set<Integer> heldItemIds()
	{
		Set<Integer> ids = new HashSet<>();
		var inventory = client.getItemContainer(InventoryID.INV);
		if (inventory == null)
			return ids;

		for (var item : inventory.getItems())
		{
			if (item.getId() != -1)
				ids.add(item.getId());
		}

		return ids;
	}

	/**
	 * Highlights a station's quick action when its spot animation appears.
	 *
	 * <p>The Alembic's quick action has a window of one tick and the Agitator's a window of two, so a counter goes
	 * 0, 1 (and 2 for the Agitator) and the highlight is removed on that station's next progress update.
	 */
	@Subscribe
	public void onGraphicsObjectCreated(GraphicsObjectCreated event)
	{
		var spotAnimId = event.getGraphicsObject().getId();

		if (!config.highlightQuickActionEvents())
			return;

		if (spotAnimId == SPOT_ANIM_ALEMBIC && alembicPotionType != null)
		{
			highlightObject(AlchemyObject.ALEMBIC, config.stationQuickActionHighlightColor());

			if (config.soundEffectAlembic())
			{
				LOGGER.debug("Playing manual found_gem sound effect");
				client.playSoundEffect(FOUND_GEM);
			}

			alembicQuickActionTicks = 1;
		}

		if (spotAnimId == SPOT_ANIM_AGITATOR && agitatorPotionType != null)
		{
			highlightObject(AlchemyObject.AGITATOR, config.stationQuickActionHighlightColor());
			agitatorQuickActionTicks = 1;
		}
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		var scriptId = event.getScriptId();
		if (scriptId != PROC_MASTERING_MIXOLOGY_BUILD_POTION_ORDERS
				&& scriptId != PROC_MASTERING_MIXOLOGY_BUILD_REAGENTS)
			return;

		var baseWidget = client.getWidget(COMPONENT_POTION_ORDERS);

		if (baseWidget == null)
			return;

		if (scriptId == PROC_MASTERING_MIXOLOGY_BUILD_POTION_ORDERS)
			updatePotionOrdersComponent(baseWidget);
		else if (config.hideOrdersPanel())
			setOrdersPanelHidden(baseWidget, true);
		else
			appendResins(baseWidget);
	}

	/**
	 * Appends each order's recipe and status to its row in the vanilla interface, colours the row from the
	 * recommendation and moves the rows to match the chosen sorting.
	 *
	 * <p>The rows are found from the interface script (Joshua-F/cs2-scripts, {@code [proc,script7063].cs2}, line 26).
	 * The recommendation is computed once per rebuild, because the orders and the resin state are both
	 * stable until the next refresh. It is null when highlighting is disabled, for instance when the toggle is off or
	 * no rewards are tracked yet.
	 *
	 * <p>The filtered children are laid out as a text for the heading, then a graphic and a text for each order:
	 * <pre>{@code
	 * TEXT - Potion Orders
	 * GRAPHIC - 5673
	 * TEXT - Mammoth-might mix
	 * GRAPHIC - 5672
	 * TEXT - <str>Mixalot</str>
	 * GRAPHIC - 5673
	 * TEXT - Marley's moonlight
	 * }</pre>
	 */
	private void updatePotionOrdersComponent(Widget baseWidget)
	{
		var children = selectChildren(baseWidget,
				widget -> widget.getType() == WidgetType.GRAPHIC || widget.getType() == WidgetType.TEXT);

		if (children.isEmpty())
			return;

		recommendedSlots = computeRecommendedSlots();

		for (int i = 0; i < potionOrders.size(); i++)
		{
			var order = potionOrders.get(i);
			LOGGER.debug("Updating component for order {}", order);
			var orderGraphic = children.get(order.idx() * 2 + 1);
			var orderText = children.get(order.idx() * 2 + 2);

			if (orderGraphic.getType() != WidgetType.GRAPHIC || orderText.getType() != WidgetType.TEXT)
			{
				LOGGER.debug("Eep Eep! Selected the wrong components!");
				continue;
			}

			orderSprites.put(order.idx(), orderGraphic.getSpriteId());
			LOGGER.debug("Order {} modifier {} sprite {}",
					order.idx(), order.potionModifier(), orderGraphic.getSpriteId());
			var builder = new StringBuilder(orderText.getText());
			boolean skipped = recommendedSlots != null && !order.fulfilled() && !recommendedSlots.contains(order.idx());
			Color skipColor = config.accessibilityMode()
					? config.accessibilitySkipColor()
					: config.notRecommendedPotionColor();

			if (order.fulfilled())
				builder.append(" (<col=00ff00>done!</col>)");
			else
				builder.append(" (")
						.append(recipeText(order.potionType(), skipped, skipColor))
						.append(")");

			orderText.setText(builder.toString());

			if (recommendedSlots != null && !order.fulfilled())
			{
				Color recommendColor = config.accessibilityMode()
						? config.accessibilityRecommendColor()
						: config.recommendedPotionColor();
				orderText.setTextColor((skipped ? skipColor : recommendColor).getRGB());
			}

			if (i != order.idx())
			{
				LOGGER.debug("Updating order {} position from {} to {}", order, order.idx(), i);
				var y = 20 + (i * 26) + 3;
				orderGraphic.setOriginalY(y);
				orderText.setOriginalY(y);

				orderGraphic.revalidate();
				orderText.revalidate();
			}
		}

		for (var child : children)
			child.setHidden(config.hideOrdersPanel());

		updateMixGuidance();
	}

	/**
	 * Builds the recipe shown after an order's name: coloured letters, or blocks in accessibility mode.
	 * A skipped order draws every letter or block in the skip colour.
	 */
	private String recipeText(PotionType potionType, boolean skipped, Color skipColor)
	{
		boolean blocks = config.accessibilityMode() && resinIconIdx != null;
		if (!skipped)
			return blocks ? potionType.recipeBlocks(resinIconIdx) : potionType.recipe();

		if (blocks)
		{
			int skipIcon = resinIconIdx[SKIP_ICON];
			return potionType.recipeBlocks(new int[]{skipIcon, skipIcon, skipIcon});
		}

		return ColorUtil.wrapWithColorTag(potionType.abbreviation(), skipColor);
	}

	/**
	 * Returns the order indices the chosen recommendation policy picks
	 * this turn, or null if highlighting is disabled (config off, no
	 * rewards selected, or order data missing).
	 *
	 * <p>The target is the summed cost of the selected rewards, which {@link Goal} already computes for the overlay
	 * and the threshold notification. The orders are placed back in their original conveyor slots, because
	 * {@code potionOrders} may be sorted by the sorting setting.
	 */
	private Set<Integer> computeRecommendedSlots()
	{
		if (!config.highlightRecommendedPotions())
			return null;

		if (potionOrders.size() != 3)
			return null;

		if (!goal.isMultiMode())
			return null;

		var mox = goal.getComponentData(MOX);
		var aga = goal.getComponentData(AGA);
		var lye = goal.getComponentData(LYE);
		if (mox == null || aga == null || lye == null)
			return null;

		int[] resin = {
				client.getVarpValue(VARP_MOX_RESIN),
				client.getVarpValue(VARP_AGA_RESIN),
				client.getVarpValue(VARP_LYE_RESIN)
		};
		int[] target = { mox.goalAmount, aga.goalAmount, lye.goalAmount };
		if (target[0] == 0 && target[1] == 0 && target[2] == 0)
			return null;

		PotionType[] ordersBySlot = new PotionType[3];
		for (PotionOrder po : potionOrders)
			ordersBySlot[po.idx()] = po.potionType();

		return recommendationPolicy.decide(ordersBySlot, resin, target);
	}

	private List<Widget> selectChildren(Widget parent, Predicate<Widget> filter)
	{
		var children = parent.getChildren();

		if (children == null)
			return List.of();

		return Arrays.stream(children)
				.filter(filter)
				.collect(Collectors.toUnmodifiableList());
	}

	/** Hides or shows every element the game draws in the orders panel, including its paste counts. */
	private void setOrdersPanelHidden(Widget baseWidget, boolean hidden)
	{
		for (var child : selectChildren(baseWidget, widget -> true))
			child.setHidden(hidden);
	}

	private void appendResins(Widget baseWidget)
	{
		if (!config.displayResin())
			return;

		var parentWidth = baseWidget.getWidth();
		var dx = parentWidth / 3;
		int x = dx / 2;

		addResinText(baseWidget.createChild(-1, WidgetType.TEXT), x, VARP_MOX_RESIN, MOX);
		addResinText(baseWidget.createChild(-1, WidgetType.TEXT), x + dx, VARP_AGA_RESIN, AGA);
		addResinText(baseWidget.createChild(-1, WidgetType.TEXT), x + dx * 2, VARP_LYE_RESIN, LYE);
	}

	private void initialize()
	{
		var ordersLayer = client.getWidget(COMPONENT_POTION_ORDERS_GROUP_ID, 0);
		if (ordersLayer == null || ordersLayer.isSelfHidden())
			return;

		LOGGER.debug("initialize plugin");
		inLab = true;
		unfinishedInInventory = countUnfinishedInInventory();
		updatePotionOrders();
		updateMixGuidance();
		tryHighlightNextStation();
	}

	/**
	 * Outlines a lab object in the given colour. The aga lever is a wall decoration rather than a scenery object, so
	 * the tile's decorative object is checked when no scenery object matches.
	 */
	public void highlightObject(AlchemyObject alchemyObject, Color color)
	{
		var worldView = client.getTopLevelWorldView();

		if (worldView == null)
			return;

		var localPoint = LocalPoint.fromWorld(worldView, alchemyObject.coordinate());

		if (localPoint == null)
			return;

		var tiles = worldView.getScene().getTiles();
		var tile = tiles[worldView.getPlane()][localPoint.getSceneX()][localPoint.getSceneY()];

		for (var gameObject : tile.getGameObjects())
		{
			if (gameObject == null)
				continue;

			if (gameObject.getId() == alchemyObject.objectId())
			{
				highlightedObjects.put(alchemyObject,
						new HighlightedObject(gameObject, color, config.highlightBorderWidth(),
								config.highlightFeather()));
				return;
			}
		}

		var decorativeObject = tile.getDecorativeObject();

		if (decorativeObject != null && decorativeObject.getId() == alchemyObject.objectId())
			highlightedObjects.put(alchemyObject,
					new HighlightedObject(decorativeObject, color, config.highlightBorderWidth(),
							config.highlightFeather()));
	}

	/**
	 * Draws the Retort in the quick-action colour while Concentrate clicks still finish the potion sooner, and back in
	 * the station colour from the last tick on.
	 */
	private void updateRetortQuickActionHighlight()
	{
		if (!config.highlightQuickActionEvents() || !retortProgressTracker.potionLoaded())
			return;

		int progress = retortProgressTracker.currentProgress();
		if (StationGuard.concentrateWorthClicking(progress, StationGuard.RETORT_PROGRESS_MAX))
			highlightObject(AlchemyObject.RETORT, config.stationQuickActionHighlightColor());
		else if (progress > 0)
			resetStationHighlight(AlchemyObject.RETORT);
	}

	public void resetStationHighlight(AlchemyObject alchemyObject)
	{
		if (config.highlightStations())
			highlightObject(alchemyObject, config.stationHighlightColor());
	}

	public void unHighlightObject(AlchemyObject alchemyObject)
	{
		highlightedObjects.remove(alchemyObject);
	}

	private void unHighlightAllStations()
	{
		unHighlightObject(AlchemyObject.RETORT);
		unHighlightObject(AlchemyObject.ALEMBIC);
		unHighlightObject(AlchemyObject.AGITATOR);
	}

	/**
	 * Recomputes the lever state from the orders, vessel and inventory, then refreshes the lever,
	 * mixing vessel and conveyor belt highlights.
	 */
	private void updateMixGuidance()
	{
		if (!inLab)
			return;

		int[] slots = mixerSlots();
		int vesselReady = client.getVarbitValue(VARBIT_MIXING_VESSEL_POTION);
		if (MixGuidance.vesselEmpty(slots, vesselReady))
			vesselCollected = false;

		if (vesselCollected)
		{
			slots = new int[slots.length];
			vesselReady = 0;
		}

		PotionOrder target = MixGuidance.nextOrderToMix(potionOrders, recommendedSlots,
				potionsOnHand(!vesselCollected));
		PotionType targetType = target == null ? null : target.potionType();
		highlightLeverState = MixGuidance.leverState(targetType, slots, vesselReady);
		int[] slotsWithPending = MixGuidance.withPendingPulls(slots, pendingLeverPulls);
		leverState = MixGuidance.leverState(targetType, slotsWithPending, vesselReady);

		updateLeverHighlights();

		boolean vesselReadyToMix = MixGuidance.vesselFull(slots, vesselReady)
				&& (!config.potionProtection() || vesselMixValid(slots, vesselReady));
		setReadyHighlight(AlchemyObject.MIXING_VESSEL, config.highlightMixingVessel() && vesselReadyToMix);

		boolean deliverable = MixGuidance.readyToDeliver(potionOrders, recommendedSlots, finishedPotionsOnHand());
		setReadyHighlight(AlchemyObject.CONVEYOR_BELT, config.highlightConveyorBelt() && deliverable);
	}

	private void setReadyHighlight(AlchemyObject alchemyObject, boolean highlighted)
	{
		if (highlighted)
			highlightObject(alchemyObject, READY_HIGHLIGHT_COLOR);
		else
			unHighlightObject(alchemyObject);
	}

	private void updateLeverHighlights()
	{
		for (PotionComponent component : PotionComponent.ENTRIES)
		{
			AlchemyObject lever = leverObject(component);
			if (shouldHighlightLever(component))
				highlightObject(lever, effectiveComponentColor(component));
			else
				unHighlightObject(lever);
		}
	}

	private boolean shouldHighlightLever(PotionComponent component)
	{
		if (!config.highlightLevers())
			return false;

		if (!config.highlightNextLeverOnly() || highlightLeverState.mode() == MixGuidance.LeverMode.FREE)
			return true;

		return highlightLeverState.mode() == MixGuidance.LeverMode.PULL && highlightLeverState.next() == component;
	}

	private static AlchemyObject leverObject(PotionComponent component)
	{
		switch (component)
		{
			case MOX:
				return MOX_LEVER;
			case AGA:
				return AGA_LEVER;
			default:
				return LYE_LEVER;
		}
	}

	private int[] mixerSlots()
	{
		return new int[]{
				client.getVarbitValue(VARBIT_MIXER_SLOT_0),
				client.getVarbitValue(VARBIT_MIXER_SLOT_1),
				client.getVarbitValue(VARBIT_MIXER_SLOT_2)
		};
	}

	/**
	 * Counts unfinished potions by type: in the inventory, loaded in a station, and (optionally) mixed in the vessel.
	 */
	private Map<PotionType, Integer> potionsOnHand(boolean includeVessel)
	{
		Map<PotionType, Integer> counts = new EnumMap<>(PotionType.class);
		var inventory = client.getItemContainer(InventoryID.INV);
		if (inventory != null)
		{
			for (var item : inventory.getItems())
			{
				var potionType = PotionType.fromItemId(item.getId());
				if (potionType != null && potionType.itemId() == item.getId())
					counts.merge(potionType, 1, Integer::sum);
			}
		}

		for (PotionType inStation : new PotionType[]{alembicPotionType, agitatorPotionType, retortPotionType})
		{
			if (inStation != null)
				counts.merge(inStation, 1, Integer::sum);
		}

		if (includeVessel)
		{
			var inVessel = PotionType.fromIdx(client.getVarbitValue(VARBIT_MIXING_VESSEL_POTION) - 1);
			if (inVessel != null)
				counts.merge(inVessel, 1, Integer::sum);
		}

		return counts;
	}

	private int countUnfinishedInInventory()
	{
		var inventory = client.getItemContainer(InventoryID.INV);
		if (inventory == null)
			return 0;

		int count = 0;
		for (var item : inventory.getItems())
		{
			var potionType = PotionType.fromItemId(item.getId());
			if (potionType != null && potionType.itemId() == item.getId())
				count++;
		}

		return count;
	}

	/**
	 * Counts finished (station-processed) potions in the inventory by type.
	 */
	private Map<PotionType, Integer> finishedPotionsOnHand()
	{
		Map<PotionType, Integer> counts = new EnumMap<>(PotionType.class);
		var inventory = client.getItemContainer(InventoryID.INV);
		if (inventory == null)
			return counts;

		for (var item : inventory.getItems())
		{
			var potionType = PotionType.fromItemId(item.getId());
			if (potionType != null && potionType.modifiedItemId() == item.getId())
				counts.merge(potionType, 1, Integer::sum);
		}

		return counts;
	}

	private void updatePotionOrders()
	{
		LOGGER.debug("Updating potion orders");
		potionOrders = getPotionOrders();

		var potionOrderSorting = config.potionOrderSorting();

		if (potionOrderSorting != PotionOrderSorting.VANILLA)
		{
			LOGGER.debug("Orders pre-sort: {}", potionOrders);
			potionOrders.sort(potionOrderSorting.comparator());
			LOGGER.debug("Sorted orders: {}", potionOrders);
		}

		updateMixGuidance();
		triggerPotionOrderUpdate();
		retryStationHighlight();
	}

	/**
	 * Highlights the next station when nothing is highlighted yet. The orders can arrive after the lab opens, for
	 * instance right after logging in, so the check made when the lab opened may have found no orders.
	 */
	private void retryStationHighlight()
	{
		if (!inLab || alembicPotionType != null || agitatorPotionType != null || retortPotionType != null)
			return;

		if (highlightedObjects.containsKey(AlchemyObject.ALEMBIC)
				|| highlightedObjects.containsKey(AlchemyObject.AGITATOR)
				|| highlightedObjects.containsKey(AlchemyObject.RETORT))
			return;

		tryHighlightNextStation();
	}

	/**
	 * Queues a fake change of the potion order varbit, which makes the client script that builds the order list run
	 * again.
	 */
	public void triggerPotionOrderUpdate()
	{
		var varbitType = client.getVarbit(VARBIT_POTION_ORDER_1);

		if (varbitType != null)
			client.queueChangedVarp(varbitType.getIndex());
	}

	private void addResinText(Widget widget, int x, int varp, PotionComponent component)
	{
		var amount = client.getVarpValue(varp);
		var color = component.color().getRGB();

		widget.setText(amount + "")
				.setTextShadowed(true)
				.setTextColor(color)
				.setOriginalWidth(20)
				.setOriginalHeight(15)
				.setFontId(FontID.QUILL_8)
				.setOriginalY(0)
				.setOriginalX(x)
				.setYPositionMode(WidgetPositionMode.ABSOLUTE_BOTTOM)
				.setXTextAlignment(WidgetTextAlignment.CENTER)
				.setYTextAlignment(WidgetTextAlignment.CENTER);

		widget.revalidate();
		LOGGER.debug("adding resin text {} at {} with color {}", amount, x, color);
	}

	private void tryFulfillOrder(PotionType potionType, PotionModifier modifier)
	{
		for (var order : potionOrders)
		{
			if (order.potionType() == potionType && order.potionModifier() == modifier && !order.fulfilled())
			{
				LOGGER.debug("Order {} has been fulfilled", order);
				order.setFulfilled(true);
				break;
			}
		}
	}

	/**
	 * True when "Wait for full order" is on and a planned order still has no potion in the inventory or a station.
	 */
	private boolean waitingForFullOrder()
	{
		return config.waitForFullOrder()
				&& MixGuidance.nextOrderToMix(potionOrders, recommendedSlots, potionsOnHand(false)) != null;
	}

	private void refreshStationHighlight()
	{
		if (!config.highlightStations() || waitingForFullOrder())
			unHighlightAllStations();
		else
			tryHighlightNextStation();
	}

	private void tryHighlightNextStation()
	{
		if (!config.highlightStations() || waitingForFullOrder())
			return;

		var inventory = client.getItemContainer(InventoryID.INV);

		if (inventory == null)
			return;

		for (var order : potionOrders)
		{
			if (order.fulfilled())
				continue;

			if (inventory.contains(order.potionType().itemId()))
			{
				LOGGER.debug("Highlighting station for order {}", order);
				highlightObject(order.potionModifier().alchemyObject(), config.stationHighlightColor());
				break;
			}
		}
	}

	private List<PotionOrder> getPotionOrders()
	{
		var potionOrders = new ArrayList<PotionOrder>(3);

		for (int orderIdx = 0; orderIdx < 3; orderIdx++)
		{
			var potionType = getPotionType(orderIdx);
			var potionModifier = getPotionModifier(orderIdx);

			if (potionType == null || potionModifier == null)
				continue;

			potionOrders.add(new PotionOrder(orderIdx, potionType, potionModifier));
		}

		return potionOrders;
	}

	private PotionType getPotionType(int orderIdx)
	{
		if (orderIdx == 0)
			return PotionType.fromIdx(client.getVarbitValue(VARBIT_POTION_ORDER_1) - 1);
		else if (orderIdx == 1)
			return PotionType.fromIdx(client.getVarbitValue(VARBIT_POTION_ORDER_2) - 1);
		else if (orderIdx == 2)
			return PotionType.fromIdx(client.getVarbitValue(VARBIT_POTION_ORDER_3) - 1);
		else
			return null;
	}

	private PotionModifier getPotionModifier(int orderIdx)
	{
		if (orderIdx == 0)
			return PotionModifier.from(client.getVarbitValue(VARBIT_POTION_MODIFIER_1) - 1);
		else if (orderIdx == 1)
			return PotionModifier.from(client.getVarbitValue(VARBIT_POTION_MODIFIER_2) - 1);
		else if (orderIdx == 2)
			return PotionModifier.from(client.getVarbitValue(VARBIT_POTION_MODIFIER_3) - 1);
		else
			return null;
	}

	public Goal getGoal()
	{
		return goal;
	}

	private void recalculateGoalData()
	{
		goal.recalculate(config, client);
	}

	/** A lab object that is outlined, with the colour, outline width and feather to draw it with. */
	public static class HighlightedObject
	{
		private final TileObject object;
		private final Color color;
		private final int outlineWidth;
		private final int feather;

		private HighlightedObject(TileObject object, Color color, int outlineWidth, int feather)
		{
			this.object = object;
			this.color = color;
			this.outlineWidth = outlineWidth;
			this.feather = feather;
		}

		public TileObject object()
		{
			return object;
		}

		public Color color()
		{
			return color;
		}

		public int outlineWidth()
		{
			return outlineWidth;
		}

		public int feather()
		{
			return feather;
		}
	}
}
