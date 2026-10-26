package work.fking.masteringmixology;

import java.awt.Color;
import java.awt.Graphics2D;
import javax.annotation.Nullable;
import javax.inject.Inject;

import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

/** Tags each potion in the inventory with its recipe, as colour blocks or, with Use Letters on, as letters. */
public class InventoryPotionOverlay extends WidgetItemOverlay
{
	private final MasteringMixologyPlugin plugin;
	private final MasteringMixologyConfig config;

	@Inject
	InventoryPotionOverlay(MasteringMixologyPlugin plugin, MasteringMixologyConfig config)
	{
		this.plugin = plugin;
		this.config = config;
		showOnInventory();
	}

	/** Draws the recipe on a potion's inventory icon, over a black drop shadow when it is drawn as letters. */
	@Override
	public void renderItemOverlay(Graphics2D graphics2D, int itemId, WidgetItem widgetItem)
	{
		if (!plugin.isInLab() || config.inventoryPotionTagType() == InventoryPotionTagType.NONE)
			return;

		var potion = PotionType.fromItemId(itemId);

		if (potion == null)
			return;

		var bounds = widgetItem.getCanvasBounds();
		var x = bounds.x + 5;
		var y = bounds.y + 30;

		if (!config.useLetters())
		{
			drawBlocks(graphics2D, potion, x, y);
			return;
		}

		drawRecipe(graphics2D, potion, x + 1, y + 1, Color.BLACK);

		if (config.inventoryPotionTagType() == InventoryPotionTagType.COLORED)
		{
			drawRecipe(graphics2D, potion, x, y, null);
			return;
		}

		drawRecipe(graphics2D, potion, x, y, Color.WHITE);
	}

	private void drawRecipe(Graphics2D graphics2D, PotionType potion, int x, int y, @Nullable Color color)
	{
		graphics2D.setFont(FontManager.getRunescapeSmallFont());

		if (color != null)
		{
			graphics2D.setColor(color);
			graphics2D.drawString(potion.abbreviation(), x, y);
			return;
		}

		for (var component : potion.components())
		{
			graphics2D.setColor(plugin.effectiveComponentColor(component));
			graphics2D.drawString(String.valueOf(component.character()), x, y);
			x += graphics2D.getFontMetrics().charWidth(component.character());
		}
	}

	private void drawBlocks(Graphics2D graphics2D, PotionType potion, int x, int y)
	{
		final int side = 7;
		int top = y - side + 1;
		for (var component : potion.components())
		{
			graphics2D.setColor(plugin.effectiveComponentColor(component));
			graphics2D.fillRect(x, top, side, side);
			graphics2D.setColor(Color.BLACK);
			graphics2D.drawRect(x, top, side, side);
			x += side + 1;
		}
	}
}
