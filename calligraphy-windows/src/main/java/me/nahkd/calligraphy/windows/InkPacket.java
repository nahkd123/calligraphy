package me.nahkd.calligraphy.windows;

import me.nahkd.calligraphy.TabletDevice;
import me.nahkd.calligraphy.packet.PacketProperty;
import me.nahkd.calligraphy.packet.PenButtonPacketProperty;
import me.nahkd.calligraphy.packet.PenPacket;

record InkPacket(InkDevice device, long timestampMillis, long timestampMicro, long[] buffer) implements PenPacket {
	@Override
	public long getTimestampMillis() {
		return timestampMillis;
	}

	@Override
	public long getTimestampMicro() {
		return timestampMicro;
	}

	@Override
	public TabletDevice getDevice() {
		return device;
	}

	@Override
	public int get(PacketProperty property) {
		if (device.statusIndex != -1) {
			// TODO convert to predefined masks
			if (property == PacketProperty.NIB_TOUCHING) return (buffer[device.statusIndex] & 0b0001) != 0 ? 1 : 0;
			if (property == PacketProperty.ERASER) return (buffer[device.statusIndex] & 0b0010) != 0 ? 1 : 0;
			if (property instanceof PenButtonPacketProperty pen && pen.index() == 0) return (buffer[device.statusIndex] & 0b1000) != 0 ? 1 : 0;
		}

		int bufferIndex = device.rangeLayout.getOrDefault(property, -1);
		if (bufferIndex == -1L) return 0;
		return (int) buffer[bufferIndex];
	}
}
