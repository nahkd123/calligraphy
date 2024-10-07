package me.nahkd.calligraphy;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import me.nahkd.calligraphy.packet.PenPacket;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.MinecraftClient;

/**
 * <p>
 * Tablet events but synchronized with game rendering thread.
 * </p>
 */
public final class TabletSyncEvents {
	private TabletSyncEvents() {}

	private static final Queue<TabletDevice> CONNECT_QUEUE = new ConcurrentLinkedQueue<>();
	private static final Queue<TabletDevice> DISCONNECT_QUEUE = new ConcurrentLinkedQueue<>();
	private static final Queue<PenPacket> PACKET_QUEUE = new ConcurrentLinkedQueue<>();

	static void init() {
		Calligraphy.getDriver().onDeviceConnect().addListener(device -> {
			if (MinecraftClient.getInstance() == null) CONNECT_QUEUE.add(device);
			else MinecraftClient.getInstance().execute(() -> CONNECT.invoker().onDeviceConnectSync(device));
		});
		Calligraphy.getDriver().onDeviceDisconnect().addListener(device -> {
			if (MinecraftClient.getInstance() == null) DISCONNECT_QUEUE.add(device);
			else MinecraftClient.getInstance().execute(() -> DISCONNECT.invoker().onDeviceDisconnectSync(device));
		});
		Calligraphy.getDriver().onPacket().addListener(packet -> {
			if (MinecraftClient.getInstance() == null) PACKET_QUEUE.add(packet);
			else MinecraftClient.getInstance().execute(() -> PACKET.invoker().onPacketSync(packet));
		});
	}

	static void releaseQueueSync() {
		while (!CONNECT_QUEUE.isEmpty()) CONNECT.invoker().onDeviceConnectSync(CONNECT_QUEUE.poll());
		while (!DISCONNECT_QUEUE.isEmpty()) DISCONNECT.invoker().onDeviceDisconnectSync(DISCONNECT_QUEUE.poll());
		while (!PACKET_QUEUE.isEmpty()) PACKET.invoker().onPacketSync(PACKET_QUEUE.poll());
	}

	public static final Event<DeviceConnectSync> CONNECT = EventFactory.createArrayBacked(DeviceConnectSync.class, listeners -> device -> {
		for (DeviceConnectSync listener : listeners) listener.onDeviceConnectSync(device);
	});

	public static final Event<DeviceDisconnectSync> DISCONNECT = EventFactory.createArrayBacked(DeviceDisconnectSync.class, listeners -> device -> {
		for (DeviceDisconnectSync listener : listeners) listener.onDeviceDisconnectSync(device);
	});

	public static final Event<PacketSync> PACKET = EventFactory.createArrayBacked(PacketSync.class, listeners -> packet -> {
		for (PacketSync listener : listeners) listener.onPacketSync(packet);
	});

	@FunctionalInterface public static interface DeviceConnectSync { void onDeviceConnectSync(TabletDevice device); }
	@FunctionalInterface public static interface DeviceDisconnectSync { void onDeviceDisconnectSync(TabletDevice device); }
	@FunctionalInterface public static interface PacketSync { void onPacketSync(PenPacket packet); }
}
