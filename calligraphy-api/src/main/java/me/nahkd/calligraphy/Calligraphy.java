package me.nahkd.calligraphy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import me.nahkd.calligraphy.api.MutablePenPacket;
import me.nahkd.calligraphy.api.PenPacket;
import me.nahkd.calligraphy.api.RawPenCallback;
import me.nahkd.calligraphy.nativelib.CalligraphyNative;
import me.nahkd.calligraphy.nativelib.OperatingSystem;

/**
 * <p>
 * The main entry point for Calligraphy API.
 * </p>
 * @see #isPlatformSupported()
 * @see #setup()
 * @see #initialize(RawPenCallback, long)
 * @see #initialize(RawPenCallback)
 */
public final class Calligraphy {
	private Calligraphy() {}

	private static MutablePenPacket lastPacketData;
	private static Path nativeLibFolder = null;

	/**
	 * <p>
	 * Setup Calligraphy: Copy the shared library to folder then load it. If the library file already exists, it
	 * will perform checksum to determine whether to replace the library with the one in JAR archive.
	 * </p>
	 * @param nativeLibFolder The folder.
	 * @see #setup()
	 */
	public static void setup(Path nativeLibFolder) {
		if (Calligraphy.nativeLibFolder != null) return;
		Calligraphy.nativeLibFolder = nativeLibFolder;
		CalligraphyNative.loadSharedLib(nativeLibFolder);
	}

	/**
	 * <p>
	 * Setup Calligraphy: Copy the shared library to temporary folder then load it using
	 * {@link System#load(String)}. If Calligraphy is already set up, this method will do nothing.
	 * </p>
	 * @see #setup(Path)
	 */
	public static void setup() {
		if (Calligraphy.nativeLibFolder != null) return;
		try {
			setup(Files.createTempDirectory("calligraphymod-api"));
		} catch (IOException e) {
			throw new RuntimeException("Unable to create temp directory", e);
		}
	}

	/**
	 * <p>
	 * Check whether the current platform/operating system have full support for reading data from
	 * pen tablets.
	 * </p>
	 * <p>
	 * Compatibility status:
	 * <li><b>Windows</b>: Supported (through Windows Ink)</li>
	 * <li><b>Linux</b>: Not supported yet (TODO: linux/input.h)</li>
	 * <li><b>MacOS</b>: Not supported yet (no known API other than OpenTabletDriver)</li>
	 * </p>
	 */
	public static boolean isPlatformSupported() {
		return CalligraphyNative.isSupported();
	}

	public static boolean isInitialized() {
		return CalligraphyNative.isInitialized();
	}

	/**
	 * <p>
	 * Initialize Calligraphy with window handle. This must be used on Windows platform; Linux and MacOS will always
	 * ignore handle value.
	 * </p>
	 * @param callback The raw pen packet callback. Can be {@code null}.
	 * @param handle The handle value. This value is HWND on Windows, and it is ignored on Linux or MacOS.
	 */
	public static void initialize(RawPenCallback callback, long handle) {
		int result = CalligraphyNative.initialize((pressure, maxPressure, tiltX, tiltY, flags) -> {
			if (callback != null) callback.callback(pressure, maxPressure, tiltX, tiltY, flags);
			if (lastPacketData == null) lastPacketData = new MutablePenPacket();
			lastPacketData.setPressure(pressure);
			lastPacketData.setMaxPressure(maxPressure);
			lastPacketData.setTiltX(tiltX);
			lastPacketData.setTiltY(tiltY);
			lastPacketData.setFlags(flags);
		}, handle);
		if (result != 0) throw new RuntimeException("An error occured from native library with code " + Integer.toHexString(result));
	}

	/**
	 * <p>
	 * Initialize Calligraphy.
	 * </p>
	 * @param callback The raw pen packet callback. Can be {@code null}.
	 * @throws UnsupportedOperationException when trying to initialize using this method on Windows. Windows
	 * requires Windows Ink listener to be attached to HWND.
	 */
	public static void initialize(RawPenCallback callback) throws UnsupportedOperationException {
		if (OperatingSystem.getCurrent() == OperatingSystem.WINDOWS)
			throw new UnsupportedOperationException("Windows platform must use initiaize(RawPenCallback, long)");
		initialize(callback, 0L);
	}

	/**
	 * <p>
	 * Uninitialize Calligraphy. The underlying native library will no longer emit events to your callback.
	 * </p>
	 */
	public static void uninitialize() {
		if (isInitialized()) return;
		CalligraphyNative.uninitialize();
	}

	/**
	 * <p>
	 * Obtain the live pen data. The object is mutable: its values are automatically modified by callback
	 * that is attached to native library.
	 * </p>
	 */
	public static PenPacket getLiveData() {
		return lastPacketData;
	}
}
