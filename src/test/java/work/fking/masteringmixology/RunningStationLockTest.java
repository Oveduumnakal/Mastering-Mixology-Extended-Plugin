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
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.overlay.OverlayManager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the Agitator and Alembic running locks through the plugin, with a mocked client: clicks on a running station
 * are consumed except during its quick-action window, which opens on the spot animation and closes on the progress
 * updates that follow.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class RunningStationLockTest
{
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
		when(config.lockAgitatorWhileRunning()).thenReturn(true);
		when(config.lockAlembicWhileRunning()).thenReturn(true);
		when(client.getWidget(InterfaceID.MM_OVERLAY, 0)).thenReturn(mock(Widget.class));

		WidgetLoaded loaded = new WidgetLoaded();
		loaded.setGroupId(InterfaceID.MM_OVERLAY);
		plugin.onWidgetLoaded(loaded);
	}

	@Test
	public void idleStationsAreNotLocked()
	{
		assertFalse(click("Agitator", AlchemyObject.AGITATOR).isConsumed());
		assertFalse(click("Alembic", AlchemyObject.ALEMBIC).isConsumed());
	}

	@Test
	public void runningAgitatorIsLockedOutsideTheQuickAction()
	{
		varbit(VarbitID.MM_LAB_AGITATOR_POTION, 1);

		assertTrue(click("Agitator", AlchemyObject.AGITATOR).isConsumed());
	}

	/** The Agitator's window stays open for two progress updates after the spot animation. */
	@Test
	public void agitatorQuickActionWindowLetsClicksThroughForTwoUpdates()
	{
		varbit(VarbitID.MM_LAB_AGITATOR_POTION, 1);
		spotAnim(SpotanimID.VFX_MACHINERY_ALCHEMY01_AGITATOR01);
		assertFalse(click("Agitator", AlchemyObject.AGITATOR).isConsumed());

		varbit(VarbitID.MM_AGITATOR_PROGRESS, 5);
		assertFalse(click("Agitator", AlchemyObject.AGITATOR).isConsumed());

		varbit(VarbitID.MM_AGITATOR_PROGRESS, 6);
		assertTrue(click("Agitator", AlchemyObject.AGITATOR).isConsumed());
	}

	/** The Alembic's window closes on the first progress update after the spot animation. */
	@Test
	public void alembicQuickActionWindowLetsClicksThroughForOneUpdate()
	{
		varbit(VarbitID.MM_LAB_ALEMBIC_POTION, 1);
		spotAnim(SpotanimID.VFX_MACHINERY_ALCHEMY01_ALEMBIC01);
		assertFalse(click("Alembic", AlchemyObject.ALEMBIC).isConsumed());

		varbit(VarbitID.MM_ALEMBIC_PROGRESS, 5);
		assertTrue(click("Alembic", AlchemyObject.ALEMBIC).isConsumed());
	}

	@Test
	public void lockOffLetsRunningStationClicksThrough()
	{
		when(config.lockAgitatorWhileRunning()).thenReturn(false);
		varbit(VarbitID.MM_LAB_AGITATOR_POTION, 1);

		assertFalse(click("Agitator", AlchemyObject.AGITATOR).isConsumed());
	}

	private void varbit(int varbitId, int value)
	{
		VarbitChanged event = new VarbitChanged();
		event.setVarbitId(varbitId);
		event.setVarpId(-1);
		event.setValue(value);
		plugin.onVarbitChanged(event);
	}

	private void spotAnim(int id)
	{
		GraphicsObject graphicsObject = mock(GraphicsObject.class);
		when(graphicsObject.getId()).thenReturn(id);
		plugin.onGraphicsObjectCreated(new GraphicsObjectCreated(graphicsObject));
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
}
