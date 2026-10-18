package com.homearrow;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

public class HomeArrowClient implements ClientModInitializer {
	public static final String MOD_ID = "homearrow";

	private static final KeyMapping.Category CATEGORY =
		KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

	private static final KeyMapping SET_HOME_KEY = KeyMappingHelper.registerKeyMapping(
		new KeyMapping("key.homearrow.set_home", InputConstants.Type.KEYSYM, InputConstants.KEY_H, CATEGORY));

	private static final KeyMapping TOGGLE_KEY = KeyMappingHelper.registerKeyMapping(
		new KeyMapping("key.homearrow.toggle_arrow", InputConstants.Type.KEYSYM, InputConstants.KEY_N, CATEGORY));

	private static boolean arrowOn = false;
	private static String hudMessage = null;
	private static long hudMessageUntil = 0;

	@Override
	public void onInitializeClient() {
		HomeStorage.load();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (SET_HOME_KEY.consumeClick()) {
				if (client.player == null || client.level == null) continue;
				HomeStorage.Home h = new HomeStorage.Home(
					client.level.dimension().toString(),
					client.player.getX(), client.player.getY(), client.player.getZ());
				HomeStorage.set(worldKey(client), h);
				arrowOn = true;
				client.player.sendSystemMessage(Component.literal(String.format(
					"Home set at %d, %d, %d - arrow is on (press N to toggle)",
					(int) Math.floor(h.x()), (int) Math.floor(h.y()), (int) Math.floor(h.z()))));
			}
			while (TOGGLE_KEY.consumeClick()) {
				if (client.player == null || client.level == null) continue;
				if (HomeStorage.get(worldKey(client)) == null) {
					showMessage("No home set yet - press H where you want your home");
				} else {
					arrowOn = !arrowOn;
					showMessage(arrowOn ? "Home arrow ON" : "Home arrow OFF");
				}
			}
		});

		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "arrow"), HomeArrowClient::render);
	}

	private static String worldKey(Minecraft mc) {
		if (mc.getSingleplayerServer() != null) {
			return "sp:" + mc.getSingleplayerServer().getWorldData().getLevelName();
		}
		if (mc.getCurrentServer() != null) {
			return "mp:" + mc.getCurrentServer().ip;
		}
		return "unknown";
	}

	private static void showMessage(String s) {
		hudMessage = s;
		hudMessageUntil = System.currentTimeMillis() + 2500;
	}

	private static void render(GuiGraphicsExtractor g, DeltaTracker dt) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return;

		int w = mc.getWindow().getGuiScaledWidth();
		int h = mc.getWindow().getGuiScaledHeight();

		if (hudMessage != null && System.currentTimeMillis() < hudMessageUntil) {
			centered(g, mc, hudMessage, w / 2, h - 70, 0xFFFFFFFF);
		}

		if (!arrowOn) return;
		HomeStorage.Home home = HomeStorage.get(worldKey(mc));
		if (home == null) return;

		int cx = w / 2;
		int cy = h / 2 - 55;

		if (!home.dimension().equals(mc.level.dimension().toString())) {
			centered(g, mc, "Your home is in another dimension", cx, cy, 0xFFFFAA00);
			return;
		}

		double dx = home.x() - mc.player.getX();
		double dz = home.z() - mc.player.getZ();
		double dist = Math.sqrt(dx * dx + dz * dz);

		if (dist < 3.0) {
			centered(g, mc, "You're home!", cx, cy, 0xFF55FF55);
			return;
		}

		// Minecraft yaw: 0 = south (+Z), 90 = west (-X). Angle from player's facing to home:
		double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
		double rel = Math.toRadians(targetYaw - mc.player.getYRot());

		// Screen direction: up = straight ahead, right = to the player's right.
		float dirX = (float) Math.sin(rel);
		float dirY = (float) -Math.cos(rel);

		float len = 18f;
		float tipX = cx + dirX * len, tipY = cy + dirY * len;
		float tailX = cx - dirX * len, tailY = cy - dirY * len;

		float wing = 11f;
		float[] w1 = rotate(-dirX, -dirY, Math.toRadians(35));
		float[] w2 = rotate(-dirX, -dirY, Math.toRadians(-35));

		// dark outline, then bright arrow on top
		drawArrow(g, tailX, tailY, tipX, tipY, w1, w2, wing, 5, 0xFF000000);
		drawArrow(g, tailX, tailY, tipX, tipY, w1, w2, wing, 3, 0xFFFFD83A);

		centered(g, mc, (int) dist + " m", cx, cy + 28, 0xFFFFFFFF);
	}

	private static void drawArrow(GuiGraphicsExtractor g, float tx, float ty, float hx, float hy,
	                              float[] w1, float[] w2, float wing, int thick, int color) {
		line(g, tx, ty, hx, hy, thick, color);
		line(g, hx, hy, hx + w1[0] * wing, hy + w1[1] * wing, thick, color);
		line(g, hx, hy, hx + w2[0] * wing, hy + w2[1] * wing, thick, color);
	}

	private static float[] rotate(float x, float y, double a) {
		float c = (float) Math.cos(a), s = (float) Math.sin(a);
		return new float[] {x * c - y * s, x * s + y * c};
	}

	private static void line(GuiGraphicsExtractor g, float x0, float y0, float x1, float y1, int thick, int color) {
		int steps = Math.max(1, (int) Math.ceil(Math.hypot(x1 - x0, y1 - y0)));
		int half = thick / 2;
		for (int i = 0; i <= steps; i++) {
			float t = i / (float) steps;
			int x = Math.round(x0 + (x1 - x0) * t);
			int y = Math.round(y0 + (y1 - y0) * t);
			g.fill(x - half, y - half, x - half + thick, y - half + thick, color);
		}
	}

	private static void centered(GuiGraphicsExtractor g, Minecraft mc, String text, int x, int y, int color) {
		g.text(mc.font, text, x - mc.font.width(text) / 2, y, color, true);
	}
}
