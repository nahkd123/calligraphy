package me.nahkd.calligraphy.api;

public record ImmutablePenPacket(short pressure, short maxPressure, short tiltX, short tiltY, long flags) implements PenPacket {
	public ImmutablePenPacket(PenPacket from) {
		this(from.pressure(), from.maxPressure(), from.tiltX(), from.tiltY(), from.flags());
	}
}
