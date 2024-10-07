package me.nahkd.calligraphy.addon.axiom.debug;

import java.util.HashMap;
import java.util.Map;

import org.joml.Matrix4fStack;

import me.nahkd.calligraphy.TabletDevice;
import me.nahkd.calligraphy.TabletSyncEvents;
import me.nahkd.calligraphy.addon.axiom.CalligraphyAxiomAddon;
import me.nahkd.calligraphy.packet.PacketProperty;
import me.nahkd.calligraphy.packet.PenPacket;
import me.nahkd.calligraphy.packet.PropertyInfo;
import me.nahkd.calligraphy.packet.RangePropertyInfo;
import me.nahkd.calligraphy.packet.SwitchPropertyInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public class CalligraphyDebugger {
	public static boolean debugging = false;
	private static Map<TabletDevice, PenPacket> lastPacket;

	public static void init() {
		CalligraphyAxiomAddon.LOGGER.info("Initializing development debug overlay...");
		debugging = true;
		lastPacket = new HashMap<>();

		TabletSyncEvents.DISCONNECT.register(packet -> lastPacket.remove(packet));
		TabletSyncEvents.PACKET.register(packet -> lastPacket.put(packet.getDevice(), packet));
	}

	@SuppressWarnings("resource")
	public static void renderDebuggingOverlay(DrawContext ctx, Matrix4fStack matrices) {
		if (!debugging) return;
		matrices.pushMatrix();
		TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
		int charHeight = textRenderer.fontHeight;
		int lines = 0;

		for (Map.Entry<TabletDevice, PenPacket> entry : CalligraphyDebugger.lastPacket.entrySet()) {
			TabletDevice device = entry.getKey();
			PenPacket last = entry.getValue();

			ctx.drawText(textRenderer, String.format("Tablet device: %s", device.getName()), 4, 4 + charHeight * lines, 0xFFFFFF, false);
			ctx.drawText(textRenderer, String.format("  Unique ID: %s", device.getUniqueId()), 4, 4 + charHeight * (lines + 1), 0xAFAFAF, false);
			lines += 2;

			for (Map.Entry<PacketProperty, PropertyInfo> infoEntry : device.getCapabilities().entrySet()) {
				String label = String.format("  %s: ", infoEntry.getKey().getName());
				int labelWidth = textRenderer.getWidth(label);
				ctx.drawText(textRenderer, label, 4, 4 + charHeight * lines, 0xAFAFAF, true);

				switch (infoEntry.getValue()) {
				case RangePropertyInfo(PacketProperty property, int min, int max, double scale, boolean abs): {
					int value = last.get(property);
					int maxWidth = 100;
					int width = (value - min) * maxWidth / (max - min);
					ctx.fill(4 + labelWidth, 4 + charHeight * lines, 4 + labelWidth + width, 4 + charHeight * (lines + 1), 0xffffc629);
					ctx.fill(4 + labelWidth + width, 4 + charHeight * lines, 4 + labelWidth + maxWidth, 4 + charHeight * (lines + 1), 0xff111b8a);
					ctx.drawText(textRenderer, String.format("%d/%d", value, max), 4 + labelWidth, 4 + charHeight * lines, 0xfff8e3, false);
					break;
				}
				case SwitchPropertyInfo(PacketProperty property): {
					int value = last.get(property);
					int width = charHeight;
					ctx.fill(4 + labelWidth, 4 + charHeight * lines, 4 + labelWidth + width, 4 + charHeight * (lines + 1), value != 0 ? 0xffffc629 : 0xff111b8a);
					break;
				}
				default:
					break;
				};

				lines++;
			}

			matrices.translate(0f, charHeight * lines, 0f);
		}

		matrices.popMatrix();
	}
}
