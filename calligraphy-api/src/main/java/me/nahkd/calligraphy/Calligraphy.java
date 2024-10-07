package me.nahkd.calligraphy;

public final class Calligraphy {
	private Calligraphy() {}

	private static TabletDriversCollection driver = new TabletDriversCollection();

	/**
	 * <p>
	 * Get the Calligraphy drivers collection. You can add driver to or remove driver from this collection.
	 * </p>
	 * @return The Calligraphy drivers collection.
	 */
	public static TabletDriversCollection getDriver() { return driver; }
}
