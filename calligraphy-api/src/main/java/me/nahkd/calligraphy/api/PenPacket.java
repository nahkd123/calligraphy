package me.nahkd.calligraphy.api;

public interface PenPacket {
	short pressure();
	short maxPressure();
	short tiltX();
	short tiltY();
	long flags();

	default boolean isEraser() { return (flags() & RawPenCallback.FLAG_ERASER) != 0; }
	default boolean isAuxButton() { return (flags() & RawPenCallback.FLAG_AUXCLICK) != 0; }
}
