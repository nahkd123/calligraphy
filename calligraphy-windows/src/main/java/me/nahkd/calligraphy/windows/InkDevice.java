package me.nahkd.calligraphy.windows;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import me.nahkd.calligraphy.TabletDevice;
import me.nahkd.calligraphy.packet.PacketProperty;
import me.nahkd.calligraphy.packet.PropertyInfo;
import me.nahkd.calligraphy.packet.RangePropertyInfo;
import me.nahkd.calligraphy.packet.SwitchPropertyInfo;

class InkDevice implements TabletDevice {
	private static final long LAYOUT_X = 0;
	private static final long LAYOUT_Y = 1;
	private static final long LAYOUT_Z = 2;
	private static final long LAYOUT_STATUS = 3;
	private static final long LAYOUT_PRESSURE = 4;
	private static final long LAYOUT_TILT_X = 5;
	private static final long LAYOUT_TILT_Y = 6;
	private static final long LAYOUT_TWIST = 7;
	private static final Map<Long, PacketProperty> WININK_TO_PROP = Map.of(
			LAYOUT_X, PacketProperty.X,
			LAYOUT_Y, PacketProperty.Y,
			LAYOUT_Z, PacketProperty.Z,
			LAYOUT_PRESSURE, PacketProperty.PRESSURE,
			LAYOUT_TILT_X, PacketProperty.TILT_X,
			LAYOUT_TILT_Y, PacketProperty.TILT_Y,
			LAYOUT_TWIST, PacketProperty.TWIST);

	private WindowsInkDriver driver;
	protected final long tcid;
	private String guid;
	private String name;
	private Map<PacketProperty, PropertyInfo> capabilities;
	protected boolean connected;

	// Layout
	protected int statusIndex = -1; // Status property index in packet received from Windows Ink
	protected Map<PacketProperty, Integer> rangeLayout; // PacketProperty => index in packet received from Windows Ink

	/**
	 * <p>
	 * Create a new InkDevice.
	 * </p>
	 * @param driver The driver object.
	 * @param tcid Tablet context ID, reported from RealTimeStylus.
	 * @param guid The GUID of the device.
	 * @param name The display name of the device.
	 * @param scaleX The scale value for X axis.
	 * @param scaleY The scale value for Y axis.
	 * @param layout The packet layout definition. Each property of a packet is 3 units of this array, where
	 * the first one is the property type, the 2 next values are lower bound and upper bound that the tablet
	 * device will report to the driver.
	 */
	public InkDevice(WindowsInkDriver driver, long tcid, String guid, String name, double scaleX, double scaleY, long[] layout) {
		this.driver = driver;
		this.tcid = tcid;
		this.guid = guid;
		this.name = name;
		this.capabilities = new HashMap<>();
		this.rangeLayout = new HashMap<>();
		this.connected = true;

		createPropInfo(PacketProperty.NIB_TOUCHING, SwitchPropertyInfo::new);
		createPropInfo(PacketProperty.ERASER, SwitchPropertyInfo::new);
		createPropInfo(PacketProperty.penButton(0), SwitchPropertyInfo::new);

		for (int i = 0; i < layout.length; i += 3) {
			long type = layout[i];

			if (type == LAYOUT_STATUS) {
				statusIndex = i / 3;
				continue;
			}

			long min = layout[i + 1], max = layout[i + 2];
			PacketProperty prop = WININK_TO_PROP.get(type);
			if (prop == null) continue; // TODO show warning
			double scale = type == LAYOUT_X ? scaleX : type == LAYOUT_Y ? scaleY : 1d;
			createPropInfo(prop, p -> new RangePropertyInfo(p, (int) min, (int) max, scale, true));
			rangeLayout.put(prop, i / 3);
		}
	}

	private <T extends PropertyInfo> T createPropInfo(PacketProperty type, Function<PacketProperty, T> applier) {
		T out = applier.apply(type);
		capabilities.put(type, out);
		return out;
	}

	@Override
	public WindowsInkDriver getDriver() {
		return driver;
	}

	@Override
	public String getUniqueId() {
		return "winink{" + guid + "}";
	}

	@Override
	public String getName() {
		return name != null ? name : guid;
	}

	@Override
	public boolean isConnected() {
		return connected;
	}

	@Override
	public Map<PacketProperty, PropertyInfo> getCapabilities() {
		return Collections.unmodifiableMap(capabilities);
	}
}
