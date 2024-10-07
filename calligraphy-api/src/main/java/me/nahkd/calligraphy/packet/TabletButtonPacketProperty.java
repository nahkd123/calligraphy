package me.nahkd.calligraphy.packet;

public record TabletButtonPacketProperty(int index) implements PacketProperty {
	@Override
	public String getName() {
		return String.format("tablet_button/", index);
	}
}
