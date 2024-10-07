package me.nahkd.calligraphy.packet;

/**
 * <p>
 * Default packet property class. For use as static final fields in {@link PacketProperty}.
 * </p>
 */
record DefaultPacketProperty(String id) implements PacketProperty {
	@Override
	public String getName() {
		return id;
	}
}
