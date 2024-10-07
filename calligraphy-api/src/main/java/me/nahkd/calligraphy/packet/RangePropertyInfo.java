package me.nahkd.calligraphy.packet;

/**
 * <p>
 * A range property info contains the range that the input device will report. The range is inclusive: the reported
 * value can be:
 * <li>Greater than or equals to minimum value;</li>
 * <li>Less than or equals to maximum value.</li>
 * </p>
 * <p>
 * <b>Components</b>:
 * <li>{@code isAbsolute}: If this boolean is false, the reported value is relative to target window. Your application
 * should handle this value accordingly (like only show tablet mapping UI if both X and Y axes are absolute for
 * example).</li>
 * </p>
 */
public record RangePropertyInfo(PacketProperty property, int minInclusive, int maxInclusive, double scale, boolean isAbsolute) implements PropertyInfo {
	public int map(int input, int toMinInclusive, int toMaxInclusive) {
		int fromRange = maxInclusive - minInclusive;
		int toRange = toMaxInclusive - toMinInclusive;
		return toMinInclusive + ((input - minInclusive) * toRange / fromRange);
	}
}
