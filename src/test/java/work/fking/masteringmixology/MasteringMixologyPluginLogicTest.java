package work.fking.masteringmixology;

import javax.inject.Inject;

import com.google.inject.Guice;
import com.google.inject.testing.fieldbinder.Bind;
import com.google.inject.testing.fieldbinder.BoundFieldModule;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import net.runelite.api.Client;
import net.runelite.api.GraphicsObject;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.ui.overlay.OverlayManager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Plugin-level tests with a mocked client and config: the click guards, the reaction to setting changes, the quick
 * action and the reward notification. The lab is entered through the orders widget loading, as in game.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class MasteringMixologyPluginLogicTest
{
	private static final int FOUND_GEM_SOUND = 2655;

	@Mock
	@Bind
	private Client client;

	@Mock
	@Bind
	private MasteringMixologyConfig config;

	@Mock
	@Bind
	private ConfigManager configManager;

	@Mock
	@Bind
	private OverlayManager overlayManager;

	@Mock
	@Bind
	private Notifier notifier;

	@Mock
	@Bind
	private ClientThread clientThread;

	@Mock
	@Bind
	private MasteringMixologyOverlay overlay;

	@Mock
	@Bind
	private InventoryPotionOverlay potionOverlay;

	@Mock
	@Bind
	private OrdersPanelOverlay ordersPanelOverlay;

	@Mock
	@Bind
	private GoalInfoBoxOverlay goalInfoBoxOverlay;

	@Inject
	private MasteringMixologyPlugin plugin;

	@Before
	public void setUp()
	{
		Guice.createInjector(BoundFieldModule.of(this)).injectMembers(this);
		when(config.potionOrderSorting()).thenReturn(PotionOrderSorting.VANILLA);
		when(config.recommendationStrategy()).thenReturn(RecommendationStrategy.EXPECTED_VALUE);
		when(client.getWidget(InterfaceID.MM_OVERLAY, 0)).thenReturn(mock(Widget.class));

		WidgetLoaded loaded = new WidgetLoaded();
		loaded.setGroupId(InterfaceID.MM_OVERLAY);
		plugin.onWidgetLoaded(loaded);
	}

	@Test
	public void retortClickIsBlockedOnTheLastTickWithTheCapOn()
	{
		when(config.capConcentrate()).thenReturn(true);
		varbit(VarbitID.MM_LAB_RETORT_POTION, 1);
		varbit(VarbitID.MM_RETORT_PROGRESS, 15);

		assertTrue(click("Retort", AlchemyObject.RETORT).isConsumed());
	}

	@Test
	public void retortClickIsAllowedWithTheCapOff()
	{
		varbit(VarbitID.MM_LAB_RETORT_POTION, 1);
		varbit(VarbitID.MM_RETORT_PROGRESS, 15);

		assertFalse(click("Retort", AlchemyObject.RETORT).isConsumed());
	}

	@Test
	public void retortClickIsAllowedEarlyWithTheCapOn()
	{
		when(config.capConcentrate()).thenReturn(true);
		varbit(VarbitID.MM_LAB_RETORT_POTION, 1);
		varbit(VarbitID.MM_RETORT_PROGRESS, 5);

		assertFalse(click("Retort", AlchemyObject.RETORT).isConsumed());
	}

	@Test
	public void idleLockBlocksAnEmptyStationNoOrderNeeds()
	{
		when(config.lockAlembicWhenIdle()).thenReturn(true);

		assertTrue(click("<col=ffff>Alembic</col>", AlchemyObject.ALEMBIC).isConsumed());
		assertFalse(click("Agitator", AlchemyObject.AGITATOR).isConsumed());
	}

	@Test
	public void idleLockAllowsAStationThatIsInUse()
	{
		when(config.lockAlembicWhenIdle()).thenReturn(true);
		varbit(VarbitID.MM_LAB_ALEMBIC_POTION, 1);

		assertFalse(click("Alembic", AlchemyObject.ALEMBIC).isConsumed());
	}

	@Test
	public void clicksOutsideTheLabAreNeverBlocked()
	{
		when(config.lockAlembicWhenIdle()).thenReturn(true);
		WidgetClosed closed = new WidgetClosed(InterfaceID.MM_OVERLAY, 0, false);
		plugin.onWidgetClosed(closed);

		assertFalse(click("Alembic", AlchemyObject.ALEMBIC).isConsumed());
	}

	@Test
	public void onlyAStrategyChangeCreatesANewPolicy()
	{
		configChanged("accessibilityMoxColor");
		verify(config, never()).recommendationStrategy();

		configChanged("recommendationStrategy");
		verify(config).recommendationStrategy();
	}

	@Test
	public void alembicQuickActionPlaysTheFixedSoundOnlyWhileCrystallising()
	{
		when(config.highlightQuickActionEvents()).thenReturn(true);
		when(config.soundEffectAlembic()).thenReturn(true);

		alembicSpotAnim();
		verify(client, never()).playSoundEffect(FOUND_GEM_SOUND);

		varbit(VarbitID.MM_LAB_ALEMBIC_POTION, 1);
		alembicSpotAnim();
		verify(client).playSoundEffect(FOUND_GEM_SOUND);
	}

	@Test
	public void rewardNotificationFiresOnceWhenTheGoalIsReached()
	{
		when(config.trackPrescriptionGoggles()).thenReturn(true);
		when(client.getVarpValue(MasteringMixologyPlugin.VARP_MOX_RESIN)).thenReturn(9000);
		when(client.getVarpValue(MasteringMixologyPlugin.VARP_AGA_RESIN)).thenReturn(9000);
		when(client.getVarpValue(MasteringMixologyPlugin.VARP_LYE_RESIN)).thenReturn(9000);
		resinChanged();
		verify(notifier, never()).notify(any(), anyString());

		when(client.getVarpValue(MasteringMixologyPlugin.VARP_LYE_RESIN)).thenReturn(9350);
		resinChanged();
		resinChanged();
		verify(notifier, times(1)).notify(any(), anyString());
	}

	private void varbit(int varbitId, int value)
	{
		VarbitChanged event = new VarbitChanged();
		event.setVarbitId(varbitId);
		event.setVarpId(-1);
		event.setValue(value);
		plugin.onVarbitChanged(event);
	}

	private void resinChanged()
	{
		VarbitChanged event = new VarbitChanged();
		event.setVarbitId(-1);
		event.setVarpId(MasteringMixologyPlugin.VARP_LYE_RESIN);
		plugin.onVarbitChanged(event);
	}

	private MenuOptionClicked click(String target, AlchemyObject object)
	{
		MenuEntry entry = mock(MenuEntry.class);
		when(entry.getType()).thenReturn(MenuAction.GAME_OBJECT_FIRST_OPTION);
		when(entry.getIdentifier()).thenReturn(object.objectId());
		when(entry.getTarget()).thenReturn(target);
		MenuOptionClicked event = new MenuOptionClicked(entry);
		plugin.onMenuOptionClicked(event);
		return event;
	}

	private void configChanged(String key)
	{
		ConfigChanged event = new ConfigChanged();
		event.setGroup(MasteringMixologyConfig.CONFIG_GROUP);
		event.setKey(key);
		plugin.onConfigChanged(event);
	}

	private void alembicSpotAnim()
	{
		GraphicsObject graphicsObject = mock(GraphicsObject.class);
		when(graphicsObject.getId()).thenReturn(SpotanimID.VFX_MACHINERY_ALCHEMY01_ALEMBIC01);
		plugin.onGraphicsObjectCreated(new GraphicsObjectCreated(graphicsObject));
	}
}
