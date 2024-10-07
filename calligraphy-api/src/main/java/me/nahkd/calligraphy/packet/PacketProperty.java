package me.nahkd.calligraphy.packet;

/**
 * <p>
 * Represent a property of the packet that's coming from the pen tablet device.
 * </p>
 */
public interface PacketProperty {
	String getName();

	// Universal properties
	/**
	 * <p>
	 * The absolute X position on the device's digitizer area.
	 * </p>
	 * <p>
	 * <b>Windows Ink note</b>: The reported position have its origin placed on top-left corner of the window,
	 * rather than the absolute position (where origin placed on top-left of the digitizer area).
	 * </p>
	 */
	PacketProperty X = new DefaultPacketProperty("x");

	/**
	 * <p>
	 * The absolute Y position on the device's digitizer area.
	 * </p>
	 * <p>
	 * <b>Windows Ink note</b>: The reported position have its origin placed on top-left corner of the window,
	 * rather than the absolute position (where origin placed on top-left of the digitizer area).
	 * </p>
	 */
	PacketProperty Y = new DefaultPacketProperty("y");

	/**
	 * <p>
	 * The absolute Z position on the device's digitizer area. Note that this was meant for devices with 3D capability 
	 * (like those 3D pens for example). If you want to measure distance or get pressure, you have to use
	 * {@link #DISTANCE} or {@link #PRESSURE}.
	 * </p>
	 */
	PacketProperty Z = new DefaultPacketProperty("z");

	/**
	 * <p>
	 * The distance between the pen and the surface of the digitizer.
	 * </p>
	 */
	PacketProperty DISTANCE = new DefaultPacketProperty("distance");

	/**
	 * <p>
	 * The raw logical pressure, which is a translation from user's physical pressure applied on the pen when it is
	 * touching a surface to an integer that sits within the range of device's capability. The pressure value might
	 * not linear (a.k.a hardware-dependent).
	 * </p>
	 */
	PacketProperty PRESSURE = new DefaultPacketProperty("pressure");

	/**
	 * <p>
	 * The tilt of the pen. The pen leans more to the left as the reported tilt value decreases, and leans more to the
	 * right as the reported tilt value increases. At zero, the pen is neither leaning to the left nor to the right.
	 * </p>
	 */
	PacketProperty TILT_X = new DefaultPacketProperty("tilt_x");

	/**
	 * <p>
	 * The tilt of the pen. The pen leans more to the top of the digitizer area as the reported tilt value decreases,
	 * and leans more to the bottom as the reported tilt value increases. At zero, the pen is neither leaning to the
	 * top nor to the bottom.
	 * </p>
	 */
	PacketProperty TILT_Y = new DefaultPacketProperty("tilt_y");

	/**
	 * <p>
	 * The twist rotation of the pen along its own axis. This is known as "barrel roll".
	 * </p>
	 */
	PacketProperty TWIST = new DefaultPacketProperty("twist");

	/**
	 * <p>
	 * The relative scroll value of the device. The value is relative to previous packet.
	 * </p>
	 */
	PacketProperty SCROLL = new DefaultPacketProperty("scroll");

	/**
	 * <p>
	 * The nib touching surface switch.
	 * </p>
	 */
	PacketProperty NIB_TOUCHING = new DefaultPacketProperty("nib_touching");

	/**
	 * <p>
	 * The eraser switch.
	 * </p>
	 */
	PacketProperty ERASER = new DefaultPacketProperty("eraser");

	/**
	 * <p>
	 * Get the property for button on the pen (as known as barrel button). Depending on the device, the property info
	 * can be {@link SwitchPropertyInfo} for regular binary state button, or {@link RangePropertyInfo} if the button
	 * is pressure-sentitive.
	 * </p>
	 * @param index The index of the pen button.
	 * @return The packet property for the pen button.
	 * @see PenButtonPacketProperty
	 */
	static PenButtonPacketProperty penButton(int index) { return new PenButtonPacketProperty(index); }

	/**
	 * <p>
	 * Get the property for button on the tablet. Depending on the device, the property info can be
	 * {@link SwitchPropertyInfo} for regular binary state button, or {@link RangePropertyInfo} if the button is
	 * pressure-sentitive.
	 * </p>
	 * @param index The index of the tablet button.
	 * @return The packet property for the tablet button.
	 * @see TabletButtonPacketProperty
	 */
	static TabletButtonPacketProperty tabletButton(int index) { return new TabletButtonPacketProperty(index); }
}
