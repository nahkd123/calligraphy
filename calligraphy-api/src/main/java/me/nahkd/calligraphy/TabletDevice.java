package me.nahkd.calligraphy;

import java.util.Map;

import me.nahkd.calligraphy.packet.PacketProperty;
import me.nahkd.calligraphy.packet.PropertyInfo;
import me.nahkd.calligraphy.packet.RangePropertyInfo;

public interface TabletDevice {
	TabletDriver getDriver();

	/**
	 * <p>
	 * Get the unique identifier of this device. The unique identifier is unique and immutable, which means you can
	 * use this in, let's say, configuration file for example.
	 * </p>
	 */
	String getUniqueId();

	/**
	 * <p>
	 * Get the user-friendly display name of this tablet device.
	 * </p>
	 */
	String getName();

	/**
	 * <p>
	 * Get the connection state of this tablet device.
	 * </p>
	 */
	boolean isConnected();

	/**
	 * <p>
	 * Get the capabilities of this device (a.k.a the limit that this device can report to the driver). Use
	 * {@code instanceof} check to see if the property is {@link RangePropertyInfo}.
	 * </p>
	 */
	Map<PacketProperty, PropertyInfo> getCapabilities();
}
