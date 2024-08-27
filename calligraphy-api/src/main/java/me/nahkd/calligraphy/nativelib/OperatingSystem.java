package me.nahkd.calligraphy.nativelib;

public enum OperatingSystem {
	WINDOWS("Windows.+", "Windows", "dll"),
	LINUX("Linux", "Linux", "so"),
	MACOS(".*Mac.*", "MacOS", "dylib"),
	UNKNOWN(null, "Unknown", "so");

	private String regex;
	private String name;
	private String libExtension;

	OperatingSystem(String regex, String name, String libExtension) {
		this.regex = regex;
		this.name = name;
		this.libExtension = libExtension;
	}

	public String getOsName() {
		return name;
	}

	public String getLibraryExtension() {
		return libExtension;
	}

	public boolean isRunningThis() {
		return regex != null && System.getProperty("os.name").matches(regex);
	}

	public static OperatingSystem getCurrent() {
		for (OperatingSystem os : values()) {
			if (os.regex != null && os.isRunningThis()) return os;
		}

		return UNKNOWN;
	}
}
