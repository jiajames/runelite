/*
 * Copyright (c) 2019, ganom <https://github.com/Ganom>
 * All rights reserved.
 * Licensed under GPL3, see LICENSE for the full scope.
 */
package net.runelite.client.plugins.autominer;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.queries.InventoryWidgetItemQuery;
import net.runelite.api.util.Text;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.flexo.Flexo;
import net.runelite.client.flexo.FlexoMouse;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginType;
import net.runelite.client.plugins.stretchedmode.StretchedModeConfig;
import net.runelite.client.util.HotkeyListener;

import javax.inject.Inject;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.runelite.client.plugins.autominer.ExtUtils;
import static net.runelite.api.ObjectID.*;

@PluginDescriptor(
	name = "Auto Miner",
	description = "Drops selected items for you.",
	tags = {"item", "drop", "dropper", "bot"},
	type = PluginType.EXTERNAL
)
@Slf4j
@SuppressWarnings("unused")
public class AutoMiner extends Plugin
{
	@Inject
	private Client client;
	@Inject
	private ConfigManager configManager;
	@Inject
	private AutoMinerConfig config;
	@Inject
	private KeyManager keyManager;
	@Inject
	private MenuManager menuManager;
	@Inject
	private ItemManager itemManager;

	private final List<WidgetItem> items = new ArrayList<>();
	private final Set<Integer> ids = new HashSet<>();
	private final Set<String> names = new HashSet<>();

	private boolean iterating;
	private int iterTicks;

	private boolean hotkeyOn;
	private int inventoryItemCount;
	private WorldPoint startingPoint;
	private Random rand = new Random();
	private int waitTicks;

	private Flexo flexo;
	private BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(1);
	private ThreadPoolExecutor executorService = new ThreadPoolExecutor(1, 1, 25, TimeUnit.SECONDS, queue,
		new ThreadPoolExecutor.DiscardPolicy());

	public int[] IRON = new int[]{ROCKS_11364, ROCKS_11365, ROCKS_36203};

	private final HotkeyListener toggle = new HotkeyListener(() -> config.toggle())
	{
		@Override
		public void hotkeyPressed()
		{
			hotkeyOn = !hotkeyOn;
			log.info("hotkey is now " + hotkeyOn);

			Item[] items = client.getItemContainer(InventoryID.INVENTORY).getItems();
			if (items == null)
			{
				inventoryItemCount = 0;
			}
			else {
				log.info("items length is " + items.length);
				inventoryItemCount = items.length;
			}

			if (hotkeyOn) {
				log.info("getting starting point");
				startingPoint = client.getLocalPlayer().getWorldLocation();
			}
			else {
				startingPoint = null;
			}
//			List<WidgetItem> list = new InventoryWidgetItemQuery()
//				.idEquals(ids)
//				.result(client)
//				.list;
//
//			items.addAll(list);
		}
	};

	@Provides
	AutoMinerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(AutoMinerConfig.class);
	}

	@Override
	protected void startUp()
	{
		Flexo.client = client;
		keyManager.registerKeyListener(toggle);
		try
		{
			flexo = new Flexo();
		}
		catch (AWTException e)
		{
			e.printStackTrace();
		}
		updateConfig();
		hotkeyOn = false;
		waitTicks = 0;
		log.info("startup");
	}

	@Override
	protected void shutDown()
	{
		keyManager.unregisterKeyListener(toggle);
		flexo = null;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals("AutoMinerConfig"))
		{
			return;
		}

		updateConfig();
	}

	private GameObject pickGameObject(int[] ids) {
		List<GameObject> objects = new ArrayList<>();
		Tile[][] tiles = client.getScene().getTiles()[client.getPlane()];
		for (int x = 0; x < Constants.SCENE_SIZE; ++x) {
			for (int y = 0; y < Constants.SCENE_SIZE; ++y) {
				Tile curTile = tiles[x][y];
				if (curTile == null)
					continue;
				GameObject[] tileObjects = curTile.getGameObjects();
				for (GameObject object : tileObjects) {
					if (object != null) {
						for (int id : ids) {
							if (object.getId() == id) {
								if (startingPoint.distanceTo(curTile.getWorldLocation()) <= 2)
									objects.add(object);
							}
						}
					}
				}
			}
		}

		log.info("" + objects.size());
		return objects.get(rand.nextInt(objects.size()));
	}

	private Rectangle centerBounds(Rectangle clickArea)
	{
		clickArea.x += clickArea.getWidth() / 2;
		clickArea.y += clickArea.getHeight() / 2;

		return clickArea;
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		log.info("game tick");
		if (!hotkeyOn)
		{
			log.info("hotkey is off");
			return;
		}

		if (startingPoint == null)
		{
			log.info("starting point is null");
			return;
		}

		WorldPoint curPoint = client.getLocalPlayer().getWorldLocation();
		if (curPoint.getX() != startingPoint.getX() || curPoint.getY() != startingPoint.getY())
		{
			log.info("character has moved since script started");
			return;
		}

		if (inventoryItemCount >= 28)
		{
			log.info("inventory is full");
			return;
		}

		GameObject rock = pickGameObject(IRON);
		Rectangle rect = rock.getConvexHull().getBounds();
		rect = centerBounds(FlexoMouse.getClickArea(rect));
		log.info(rect.x + " " + rect.y);

		waitTicks++;
		if (waitTicks > 5)
		{
			ExtUtils.handleSwitch(
					rect,
					config.actionType(),
					flexo,
					client,
					configManager.getConfig(StretchedModeConfig.class).scalingFactor()
			);
			waitTicks = 0;
		}
//		if (items.isEmpty())
//		{
//			if (iterating)
//			{
//				iterTicks++;
//				if (iterTicks > 20)
//				{
//					iterating = false;
//					clearNames();
//				}
//			}
//			else
//			{
//				if (iterTicks > 0)
//				{
//					iterTicks = 0;
//				}
//			}
//			return;
//		}

		//dropItems(items);
		//items.clear();
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getItemContainer() != client.getItemContainer(InventoryID.INVENTORY))
		{
			return;
		}

		int quant = 0;

		Item[] items = event.getItemContainer().getItems();
		if (items == null)
		{
			inventoryItemCount = 0;
		}
		else
		{
			log.info("items length is " + items.length);
			inventoryItemCount = items.length;
		}

		for (Item item : event.getItemContainer().getItems())
		{
			if (ids.contains(item.getId()))
			{
				quant++;
			}
		}

		if (iterating && quant == 0)
		{
			iterating = false;
			clearNames();
		}

	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			updateConfig();
		}
	}

	private void dropItems(List<WidgetItem> dropList)
	{
		iterating = true;

		for (String name : names)
		{
			menuManager.addPriorityEntry("drop", name);
			menuManager.addPriorityEntry("release", name);
			menuManager.addPriorityEntry("destroy", name);
		}

		List<Rectangle> rects = new ArrayList<>();

		for (WidgetItem item : dropList)
		{
			rects.add(item.getCanvasBounds());
		}

		executorService.submit(() ->
		{
			for (Rectangle rect : rects)
			{
				ExtUtils.handleSwitch(
					rect,
					config.actionType(),
					flexo,
					client,
					configManager.getConfig(StretchedModeConfig.class).scalingFactor()
				);

				try
				{
					Thread.sleep((int) getMillis());
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		});
	}

	private long getMillis()
	{
		return (long) (Math.random() * config.randLow() + config.randHigh());
	}

	private void updateConfig()
	{
		ids.clear();

		for (int i : ExtUtils.stringToIntArray(config.items()))
		{
			ids.add(i);
		}

		clearNames();

		if (client.getGameState() == GameState.LOGGED_IN)
		{
			names.clear();

			for (int i : ids)
			{
				final String name = Text.standardize(itemManager.getItemDefinition(i).getName());
				names.add(name);
			}
		}
	}

	private void clearNames()
	{
		for (String name : names)
		{
			menuManager.removePriorityEntry("drop", name);
			menuManager.removePriorityEntry("release", name);
			menuManager.removePriorityEntry("destroy", name);
		}
	}
}
