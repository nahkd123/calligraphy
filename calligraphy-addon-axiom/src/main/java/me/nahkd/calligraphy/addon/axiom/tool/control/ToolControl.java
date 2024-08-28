package me.nahkd.calligraphy.addon.axiom.tool.control;

import com.mojang.serialization.Codec;

public interface ToolControl<T> {
	String getId();
	T get();
	void set(T value);
	void imgui();

	// Saving and loading
	/**
	 * <p>
	 * Get the codec for value. This will be used for saving or loading the control value.
	 * </p>
	 * @return The value codec.
	 */
	Codec<T> getValueCodec();
}
