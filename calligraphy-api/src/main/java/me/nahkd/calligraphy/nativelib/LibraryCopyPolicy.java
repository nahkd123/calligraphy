package me.nahkd.calligraphy.nativelib;

public enum LibraryCopyPolicy {
	/**
	 * <p>Always copy the library, regardless the file in folder.</p>
	 */
	ALWAYS,
	/**
	 * <p>Only copy the library if the checksum does not match or the file does not exists.</p>
	 */
	CHANGED_ONLY,
	/**
	 * <p>Only copy the library if the file does not exists.</p>
	 */
	ABSENT_ONLY;
}
