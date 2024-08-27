package me.nahkd.calligraphy.api;

/**
 * <p>
 * A functional interface for raw pen data callback. The callback will be called <b>inside</b> input thread (not
 * main thread!).
 * </p>
 */
@FunctionalInterface
public interface RawPenCallback {
	/**
	 * <p>Active when user is putting their eraser near the input device.</p>
	 */
	static int FLAG_ERASER = 0b0001;

	/**
	 * <p>Active when user is holding the side button/auxiliary button (also known as right click button)</p>
	 */
	static int FLAG_AUXCLICK = 0b0010;

	/**
	 * <p>
	 * Call the callback method with raw pen data.
	 * </p>
	 * @param pressure The reported pressure from the input device.
	 * @param maxPressure The maximum pressure reported from the input device. Usually a constant value, unless
	 * user have more than 1 tablet with each having varying maximum pressure.
	 * @param tiltX The tilting angle.
	 * @param tiltY The tilting angle.
	 * @param flags The bit flags. See constants in {@link RawPenCallback} class for all flags.
	 */
	void callback(short pressure, short maxPressure, short tiltX, short tiltY, long flags);
}
