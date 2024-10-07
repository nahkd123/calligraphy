package me.nahkd.calligraphy;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import me.nahkd.calligraphy.event.CollectionBackedEmitter;
import me.nahkd.calligraphy.event.Emitter;
import me.nahkd.calligraphy.packet.PenPacket;

public final class TabletDriversCollection implements TabletDriver, Iterable<TabletDriver> {
	private Set<TabletDriver> drivers = new HashSet<>();
	private Set<TabletDevice> devices = new HashSet<>();
	private CollectionBackedEmitter<TabletDevice> connectEmitter = new CollectionBackedEmitter<>();
	private CollectionBackedEmitter<TabletDevice> disconnectEmitter = new CollectionBackedEmitter<>();
	private CollectionBackedEmitter<PenPacket> packetEmitter = new CollectionBackedEmitter<>();

	public boolean addDriver(TabletDriver driver) {
		if (!drivers.add(driver)) return false;
		for (TabletDevice device : driver.getConnectedDevices()) driverDeviceConnectCallback(device);
		driver.onDeviceConnect().addListener(this::driverDeviceConnectCallback);
		driver.onDeviceDisconnect().addListener(this::driverDeviceDisconnectCallback);
		driver.onPacket().addListener(this::driverDevicePacketCallback);
		return true;
	}

	public boolean removeDriver(TabletDriver driver) {
		if (!drivers.remove(driver)) return false;
		driver.onDeviceConnect().removeListener(this::driverDeviceConnectCallback);
		driver.onDeviceDisconnect().removeListener(this::driverDeviceDisconnectCallback);
		driver.onPacket().removeListener(this::driverDevicePacketCallback);
		for (TabletDevice device : driver.getConnectedDevices()) driverDeviceDisconnectCallback(device);
		return true;
	}

	public int getDriversCount() {
		return drivers.size();
	}

	@Override
	public Iterator<TabletDriver> iterator() {
		return Collections.unmodifiableSet(drivers).iterator();
	}

	private void driverDeviceConnectCallback(TabletDevice device) {
		devices.add(device);
		connectEmitter.emit(device);
	}

	private void driverDeviceDisconnectCallback(TabletDevice device) {
		devices.remove(device);
		disconnectEmitter.emit(device);
	}

	private void driverDevicePacketCallback(PenPacket packet) {
		packetEmitter.emit(packet);
	}

	@Override public Collection<? extends TabletDevice> getConnectedDevices() { return Collections.unmodifiableCollection(devices); }
	@Override public Emitter<TabletDevice> onDeviceConnect() { return connectEmitter; }
	@Override public Emitter<TabletDevice> onDeviceDisconnect() { return disconnectEmitter; }
	@Override public Emitter<PenPacket> onPacket() { return packetEmitter; }
}
