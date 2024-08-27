package me.nahkd.calligraphy.api;

public class MutablePenPacket implements PenPacket {
	private short pressure = 0, maxPressure = 1, tiltX = 0, tiltY = 0;
	private long flags = 0L;

	public void setPressure(short pressure) {
		this.pressure = pressure;
	}

	public void setMaxPressure(short maxPressure) {
		this.maxPressure = maxPressure;
	}

	public void setTiltX(short tiltX) {
		this.tiltX = tiltX;
	}

	public void setTiltY(short tiltY) {
		this.tiltY = tiltY;
	}

	public void setFlags(long flags) {
		this.flags = flags;
	}

	@Override
	public short pressure() {
		return pressure;
	}

	@Override
	public short maxPressure() {
		return maxPressure;
	}

	@Override
	public short tiltX() {
		return tiltX;
	}

	@Override
	public short tiltY() {
		return tiltY;
	}

	@Override
	public long flags() {
		return flags;
	}
}
