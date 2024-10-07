package me.nahkd.calligraphy.windows;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import me.nahkd.calligraphy.TabletDriver;
import me.nahkd.calligraphy.event.CollectionBackedEmitter;
import me.nahkd.calligraphy.event.Emitter;
import me.nahkd.calligraphy.packet.PenPacket;

public class WindowsInkDriver implements TabletDriver, AutoCloseable {
	// Native loading
	private static Path copyDllDest;
	private static boolean loaded = false;
	private static boolean supported = System.getProperty("os.name").contains("Windows");

	public static void setup(Path copyDllDest) {
		WindowsInkDriver.copyDllDest = copyDllDest;
	}

	public static void init() {
		if (loaded || !supported) return;

		try {
			if (copyDllDest == null) copyDllDest = Files.createTempDirectory("calligraphy-windows-ink");
			Architecture arch = Architecture.getCurrent();
			String filename = "calligraphy-winink.dll";
			String resName = "natives/windows/" + arch.getFolderName() + "/" + filename;

			Path copyTo = copyDllDest.resolve(arch.getFolderName()).resolve(filename);
			if (!Files.exists(copyTo.resolve(".."))) Files.createDirectories(copyTo.resolve(".."));

			try (InputStream stream = WindowsInkDriver.class.getClassLoader().getResourceAsStream(resName)) {
				if (stream == null) {
					supported = false;
					loaded = true;
					return;
				}

				Files.copy(stream, copyTo, StandardCopyOption.REPLACE_EXISTING);
			}

			System.load(copyTo.toAbsolutePath().normalize().toString());
			supported = true;
			loaded = true;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * <p>
	 * Check whether this system is supported. Should be {@code true} on Windows x64 and {@code false} for other
	 * OS + architectures.
	 * </p>
	 * <p>
	 * If the system is not supported, Windows Ink Calligraphy adapter would still works, but it will not detect any
	 * devices.
	 * </p>
	 * @return Supported state.
	 */
	public static boolean isSupported() {
		if (!loaded) init();
		return supported;
	}

	// Instance
	private long nativeId = -1;
	private Map<Long, InkDevice> devices = new HashMap<>();
	private boolean destroyed = false;
	private CollectionBackedEmitter<InkDevice> connectEmitter = new CollectionBackedEmitter<>();
	private CollectionBackedEmitter<InkDevice> disconnectEmitter = new CollectionBackedEmitter<>();
	private CollectionBackedEmitter<PenPacket> packetEmitter = new CollectionBackedEmitter<>();

	public WindowsInkDriver(long hwnd) {
		init();
		if (supported) nativeId = nativeInit(hwnd);
	}

	@Override
	public Collection<InkDevice> getConnectedDevices() {
		if (destroyed) throw new IllegalStateException("Driver already destroyed");
		return devices.values();
	}

	@Override public Emitter<InkDevice> onDeviceConnect() { return connectEmitter; }
	@Override public Emitter<InkDevice> onDeviceDisconnect() { return disconnectEmitter; }
	@Override public Emitter<PenPacket> onPacket() { return packetEmitter; }

	@Override
	public void close() {
		if (!destroyed) nativeDestroy(nativeId);
		destroyed = true;
		nativeId = -1;
	}

	// Native methods
	private native long nativeInit(long hwnd);
	private native void nativeDestroy(long nativeId);

	// Native callbacks
	private void nativeRtsEnable() {
		// Remove old tablets just in case
		// Normally when the RTS is disabled, it will call nativeTabletDisconnect() on all tcids.
		devices.values().forEach(device -> {
			device.connected = false;
			disconnectEmitter.emit(device);
		});
		devices.clear();
	}

	private void nativeTabletAdd(long tcid, String guid, String name, double scaleX, double scaleY, long[] layout) {
		InkDevice device = new InkDevice(this, tcid, guid, name, scaleX, scaleY, layout);
		devices.put(tcid, device);
		connectEmitter.emit(device);
	}

	private void nativeTabletDisconnect(long tcid) {
		InkDevice device = devices.remove(tcid);
		if (device == null) return;
		device.connected = false;
		disconnectEmitter.emit(device);
	}

	private void nativeTabletPacket(long tcid, long[] data) {
		InkDevice device = devices.get(tcid);
		if (device == null) return; // TODO warning
		InkPacket packet = new InkPacket(device, 0, 0, data);
		packetEmitter.emit(packet);
	}
}
