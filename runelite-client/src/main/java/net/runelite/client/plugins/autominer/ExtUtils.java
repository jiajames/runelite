/*
 * Copyright (c) 2019, ganom <https://github.com/Ganom>
 * All rights reserved.
 * Licensed under GPL3, see LICENSE for the full scope.
 */
package net.runelite.client.plugins.autominer;

import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.*;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Point;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.flexo.Flexo;

import net.runelite.client.plugins.autominer.ActionType;

@Slf4j
public class ExtUtils
{
	public static int[] stringToIntArray(String string)
	{
		return Arrays.stream(string.split(",")).map(String::trim).mapToInt(Integer::parseInt).toArray();
	}

	public static List<WidgetItem> getItems(int[] itemIds, Client client)
	{
		Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);

		ArrayList<Integer> itemIDs = new ArrayList<>();

		for (int i : itemIds)
		{
			itemIDs.add(i);
		}

		List<WidgetItem> listToReturn = new ArrayList<>();

		for (WidgetItem item : inventoryWidget.getWidgetItems())
		{
			if (itemIDs.contains(item.getId()))
			{
				listToReturn.add(item);
			}
		}

		return listToReturn;
	}

	public static List<Widget> getEquippedItems(int[] itemIds, Client client)
	{
		Widget equipmentWidget = client.getWidget(WidgetInfo.EQUIPMENT);

		ArrayList<Integer> equippedIds = new ArrayList<>();

		for (int i : itemIds)
		{
			equippedIds.add(i);
		}

		List<Widget> equipped = new ArrayList<>();

		if (equipmentWidget.getStaticChildren() != null)
		{
			for (Widget widgets : equipmentWidget.getStaticChildren())
			{
				for (Widget items : widgets.getDynamicChildren())
				{
					if (equippedIds.contains(items.getItemId()))
					{
						equipped.add(items);
					}
				}
			}
		}
		else
		{
			log.error("Static Children is Null!");
		}

		return equipped;
	}

	public static void handleSwitch(Rectangle rectangle, ActionType actionType, Flexo flexo, Client client, double scalingfactor)
	{
		Point cp = getClickPoint(rectangle, scalingfactor, client.isStretchedEnabled());
		log.info(cp.getX() + " " + cp.getY());
		Point tmp = client.getMouseCanvasPosition();
		log.info(tmp.getX() + " " + tmp.getY());
		java.awt.Point mousePos = new java.awt.Point(tmp.getX(), tmp.getY());

		if (cp.getX() >= 1 && cp.getY() >= 1)
		{
			switch (actionType)
			{
				case FLEXO:
					if (!rectangle.contains(mousePos))
					{
						flexo.mouseMove(cp.getX(), cp.getY());
					}
//					flexo.mousePressAndRelease(1);
					pauseMS(getMinDelay());
										flexo.mousePressAndRelease(1);
					//leftClick(cp.getX(), cp.getY(), client, scalingfactor);
					break;
				case MOUSEEVENTS:
					if (!rectangle.contains(mousePos))
					{
						moveMouse(cp.getX(), cp.getY(), client);
					}
					leftClick(cp.getX(), cp.getY(), client, scalingfactor);
					break;
			}
		}
	}

	private static void pauseMS(int delayMS)
	{
		long initialMS = System.currentTimeMillis();
		while (System.currentTimeMillis() < initialMS + delayMS)
		{
			try
			{
				Thread.sleep(10);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	private static int getMinDelay()
	{
		int minDelay = 45;
		Random random = new Random();
		int random1 = random.nextInt(minDelay);
		if (random1 < minDelay / 2)
		{
			random1 = random.nextInt(minDelay / 2) + minDelay / 2 + random.nextInt(minDelay / 2);
		}
		return random1;
	}

	public static void leftClick(int x, int y, Client client, double scalingFactor)
	{
		if (client.isStretchedEnabled())
		{
			double scale = 1 + (scalingFactor / 100);

			MouseEvent mousePressed =
				new MouseEvent(client.getCanvas(), 501, System.currentTimeMillis(), 0, (int) (client.getMouseCanvasPosition().getX() * scale), (int) (client.getMouseCanvasPosition().getY() * scale), 1, false, 1);
			client.getCanvas().dispatchEvent(mousePressed);
			pauseMS(getMinDelay());
			MouseEvent mouseReleased =
				new MouseEvent(client.getCanvas(), 502, System.currentTimeMillis(), 0, (int) (client.getMouseCanvasPosition().getX() * scale), (int) (client.getMouseCanvasPosition().getY() * scale), 1, false, 1);
			client.getCanvas().dispatchEvent(mouseReleased);
			pauseMS(getMinDelay());
			MouseEvent mouseClicked =
				new MouseEvent(client.getCanvas(), 500, System.currentTimeMillis(), 0, (int) (client.getMouseCanvasPosition().getX() * scale), (int) (client.getMouseCanvasPosition().getY() * scale), 1, false, 1);
			client.getCanvas().dispatchEvent(mouseClicked);
		}
		if (!client.isStretchedEnabled())
		{
			MouseEvent mousePressed =
				new MouseEvent(client.getCanvas(), 501, System.currentTimeMillis(), 0, client.getMouseCanvasPosition().getX(), client.getMouseCanvasPosition().getY(), 1, false, 1);
			client.getCanvas().dispatchEvent(mousePressed);
			pauseMS(getMinDelay());
			MouseEvent mouseReleased =
				new MouseEvent(client.getCanvas(), 502, System.currentTimeMillis(), 0, client.getMouseCanvasPosition().getX(), client.getMouseCanvasPosition().getY(), 1, false, 1);
			client.getCanvas().dispatchEvent(mouseReleased);
			pauseMS(getMinDelay());
			MouseEvent mouseClicked =
				new MouseEvent(client.getCanvas(), 500, System.currentTimeMillis(), 0, client.getMouseCanvasPosition().getX(), client.getMouseCanvasPosition().getY(), 1, false, 1);
			client.getCanvas().dispatchEvent(mouseClicked);
		}
	}

	private static void moveMouse(int x, int y, Client client)
	{
		MouseEvent mouseEntered = new MouseEvent(client.getCanvas(), 504, System.currentTimeMillis(), 0, x, y, 0, false);
		client.getCanvas().dispatchEvent(mouseEntered);
		MouseEvent mouseExited = new MouseEvent(client.getCanvas(), 505, System.currentTimeMillis(), 0, x, y, 0, false);
		client.getCanvas().dispatchEvent(mouseExited);
		MouseEvent mouseMoved = new MouseEvent(client.getCanvas(), 503, System.currentTimeMillis(), 0, x, y, 0, false);
		client.getCanvas().dispatchEvent(mouseMoved);
	}

	private static int getRandomIntBetweenRange(int min, int max)
	{
		return (int) ((Math.random() * ((max - min) + 1)) + min);
	}

	private static Point getClickPoint(Rectangle rect, double scalingFactor, boolean stretchedMode)
	{
//		if (stretchedMode)
//		{
//			int rand = (Math.random() <= 0.5) ? 1 : 2;
//			int x = (int) (rect.getX() + (rand * 3) + rect.getWidth() / 2);
//			int y = (int) (rect.getY() + (rand * 3) + rect.getHeight() / 2);
//			double scale = 1 + (scalingFactor / 100);
//			return new Point((int) (x * scale), (int) (y * scale));
//		}
//		else
//		{
//			int rand = (Math.random() <= 0.5) ? 1 : 2;
//			int x = (int) (rect.getX() + (rand * 3) + rect.getWidth() / 2);
//			int y = (int) (rect.getY() + (rand * 3) + rect.getHeight() / 2);
//			return new Point(x, y);
//		}
		int x = (int) (rect.getX() + getRandomIntBetweenRange((int) (rect.getWidth() / 6 * -1) + 1, (int) ((rect.getWidth() / 6) + rect.getWidth() / 2) - 1));
		int y = (int) (rect.getY() + getRandomIntBetweenRange((int) (rect.getHeight() / 6 * -1) + 1, (int) ((rect.getHeight() / 6) + rect.getHeight() / 2) - 1));

		if (stretchedMode)
		{
			double scale = 1 + (scalingFactor / 100);
			return new Point((int) (x * scale), (int) (y * scale));
		}

		return new Point(x, y);
	}

	public static GameObject grabNearestObject(int id, List<GameObject> objects, java.awt.Point playerPoint)
	{
		List<Double> tmp = new ArrayList<>();
		final GameObject[] temp = {null};

		objects.forEach(object ->
		{
			if (object.getId() == id)
			{
				final Rectangle b = object.getConvexHull().getBounds();
				final double distance = distance(playerPoint.x, playerPoint.y, (int) b.getCenterX(), (int) b.getCenterY());
				tmp.add(distance);
				double lowest = Collections.min(tmp);

				log.info("Distance {}, Lowest {}", distance, lowest);

				if (distance == lowest)
				{
					temp[0] = object;
				}
			}
		});

		return temp[0];
	}

	private static double distance(int x1, int y1, int x2, int y2)
	{
		int dx = x2 - x1;
		int dy = y2 - y1;
		return (Math.sqrt(dx * dx + dy * dy));
	}
}