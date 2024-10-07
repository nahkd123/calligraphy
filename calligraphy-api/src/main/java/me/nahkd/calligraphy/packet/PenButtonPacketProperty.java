package me.nahkd.calligraphy.packet;

public record PenButtonPacketProperty(int index) implements PacketProperty {
	@Override
	public String getName() {
		return String.format("pen_button/%d", index);
	}
}
