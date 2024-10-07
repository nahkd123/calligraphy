package me.nahkd.calligraphy.windows;

enum Architecture {
	X86_64("x86-64"),
	ARM64("arm64"),
	UNKNOWN(null);

	private String filenameSuffix;

	Architecture(String filenameSuffix) {
		this.filenameSuffix = filenameSuffix;
	}

	public String getFolderName() {
		return filenameSuffix;
	}

	public static Architecture getCurrent() {
		return switch (System.getProperty("os.arch")) {
		case "amd64" -> X86_64;
		case "aarch64", "arm64" -> ARM64;
		default -> UNKNOWN;
		};
	}
}
