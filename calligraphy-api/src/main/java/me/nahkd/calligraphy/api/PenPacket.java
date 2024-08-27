package me.nahkd.calligraphy.api;

public interface PenPacket {
	short pressure();
	short maxPressure();
	short tiltX();
	short tiltY();
	long flags();
}
