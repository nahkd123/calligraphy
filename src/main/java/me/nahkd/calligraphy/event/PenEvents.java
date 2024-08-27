package me.nahkd.calligraphy.event;

import me.nahkd.calligraphy.api.RawPenCallback;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public final class PenEvents {
	private PenEvents() {}

	public static final Event<RawPenCallback> RAW = EventFactory.createArrayBacked(RawPenCallback.class, callbacks -> (pressure, maxPressure, tiltX, titlY, flags) -> {
		for (RawPenCallback callback : callbacks) callback.callback(pressure, maxPressure, tiltX, titlY, flags);
	});
}
