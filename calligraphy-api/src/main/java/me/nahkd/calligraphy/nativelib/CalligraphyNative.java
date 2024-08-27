package me.nahkd.calligraphy.nativelib;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import me.nahkd.calligraphy.api.RawPenCallback;

/**
 * <p>
 * The native part of Calligraphy. Avoid using this whenever possible.
 * </p>
 */
public final class CalligraphyNative {
	private CalligraphyNative() {} // Static class

	public static native boolean isSupported();
	public static native boolean isInitialized();
	public static native int initialize(RawPenCallback callback, long handle);
	public static native void uninitialize();

	public static void loadSharedLib(Path libCopyFolder, OperatingSystem os, LibraryCopyPolicy policy) throws IOException {
		Path libOsFolder = libCopyFolder.resolve(os.getOsName());
		Path libPath = libOsFolder.resolve("calligraphy-native." + os.getLibraryExtension());
		String resource = "calligraphy-native/" + os.getOsName() + "/calligraphy-native." + os.getLibraryExtension();
		if (!Files.exists(libOsFolder)) Files.createDirectories(libOsFolder);

		if (policy == LibraryCopyPolicy.ABSENT_ONLY && Files.exists(libPath)) return;
		if (policy == LibraryCopyPolicy.CHANGED_ONLY && Files.exists(libPath)) {
			try (
					InputStream stream = Files.newInputStream(libPath);
					InputStream resStream = CalligraphyNative.class.getClassLoader().getResourceAsStream(resource)) {
				byte[] current = digest(stream);
				byte[] res = digest(resStream);
				boolean valid = true;

				for (int i = 0; i < current.length; i++) {
					if (current[i] != res[i]) {
						valid = false;
						break;
					}
				}

				if (valid) return;
			}
		}

		try (
				OutputStream outStream = Files.newOutputStream(libPath);
				InputStream inStream = CalligraphyNative.class.getClassLoader().getResourceAsStream(resource)) {
			inStream.transferTo(outStream);
		}

		System.load(libPath.toAbsolutePath().normalize().toString());
	}

	public static void loadSharedLib(Path libCopyFolder) {
		try {
			loadSharedLib(libCopyFolder, OperatingSystem.getCurrent(), LibraryCopyPolicy.ALWAYS);
		} catch (IOException e) {
			throw new RuntimeException("Unable to load shared library", e);
		}
	}

	public static void loadSharedLib() {
		try {
			loadSharedLib(Files.createTempDirectory("calligraphymod"), OperatingSystem.getCurrent(), LibraryCopyPolicy.ALWAYS);
		} catch (IOException e) {
			throw new RuntimeException("Unable to load shared library", e);
		}
	}

	private static byte[] digest(InputStream stream) throws IOException {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] buf = new byte[4096];

			while (stream.available() > 0) {
				int bytesRead = stream.read(buf);
				md.update(buf, 0, bytesRead);
			}

			return md.digest();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Your JRE does not support SHA-1 hashing algorithm!");
		}
	}
}
