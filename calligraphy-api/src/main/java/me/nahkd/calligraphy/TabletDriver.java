package me.nahkd.calligraphy;

import java.util.Collection;

import me.nahkd.calligraphy.event.Emitter;
import me.nahkd.calligraphy.packet.PenPacket;

/**
 * <p>
 * Represent the driver that's driving the pen tablet devices. Each platform have its own different driver.
 * On Windows, it will be Windows Ink. On Linux, it will be reading data directly from {@code /dev/input/event#} (where
 * {@code #} is the index of the input device). On MacOS, there is OpenTabletDriver, but further setup is required.
 * </p>
 * <p>
 * Supported drivers:
 * <li>Windows Ink (x86_64 + ARM64). <b>See note at bottom of this JD</b>.</li>
 * <li>Linux ({@code /dev/input}, all architectures)</li>
 * <li>OpenTabletDriver (repackaged as shared library): Windows, Linux and MacOS (x86_64 + ARM64)</li>
 * </p>
 * <p>
 * <b>Windows Ink note</b>: In the case of Windows Ink, the driver can only be initialized by creating the driver
 * manually through constructor with the HwND of the window you want to listen for events. You also need to close the
 * driver manually once you are done with it to free up resources.
 * </p>
 */
public interface TabletDriver {
	/**
	 * <p>
	 * Get all {@link TabletDevice} that are connected.
	 * </p>
	 */
	Collection<? extends TabletDevice> getConnectedDevices();

	/**
	 * <p>
	 * Get the emitter that emits when a pen tablet is connected.
	 * </p>
	 */
	Emitter<? extends TabletDevice> onDeviceConnect();

	/**
	 * <p>
	 * Get the emitter that emits when a pen tablet is disconnected.
	 * </p>
	 */
	Emitter<? extends TabletDevice> onDeviceDisconnect();

	/**
	 * <p>
	 * Get the emitter that emits when a packet is received from any pen tablet recognized by this driver. This emitter
	 * will always emit to your listeners in driver thread.
	 * </p>
	 */
	Emitter<? extends PenPacket> onPacket();
}
