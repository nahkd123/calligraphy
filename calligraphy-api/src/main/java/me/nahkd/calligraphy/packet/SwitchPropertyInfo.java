package me.nahkd.calligraphy.packet;

/**
 * <p>
 * A switch property reports the value in binary: either on (for holding down a button/switch) or off (for the
 * opposite). If the reported property value is zero, the switch is not being held down. If the value is not zero,
 * the switch is being held down by user.
 * </p>
 * <p>
 * The property info for pressure-sensitive switches must be {@link RangePropertyInfo}; this property info is for
 * binary input only.
 * </p>
 */
public record SwitchPropertyInfo(PacketProperty property) implements PropertyInfo {
}
