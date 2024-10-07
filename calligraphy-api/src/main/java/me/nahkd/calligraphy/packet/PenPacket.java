package me.nahkd.calligraphy.packet;

import me.nahkd.calligraphy.TabletDevice;

/**
 * <p>
 * Represent a packet (a.k.a report) that's reported from the input device.
 * </p>
 */
public interface PenPacket {
	long getTimestampMillis();
	long getTimestampMicro();

	/**
	 * <p>
	 * Get the device that reported this packet to driver. You can get a list of properties that this packet
	 * contains, as well as their limits.
	 * </p>
	 */
	TabletDevice getDevice();

	/**
	 * <p>
	 * Get the property value of this packet. If such value doesn't exists, it will returns 0.
	 * </p>
	 * @param property The property.
	 * @return The property value.
	 */
	int get(PacketProperty property);

	default void get(int[] buf, int off, PacketProperty... properties) {
		if (off < 0) throw new IndexOutOfBoundsException("off < 0");
		if (off + properties.length > buf.length) throw new IndexOutOfBoundsException("off + properties.length > buf.length");
		for (int i = 0; i < properties.length; i++) buf[off + i] = get(properties[i]);
	}
}
