package me.nahkd.calligraphy.event;

import java.util.function.Consumer;

public interface Emitter<T> {
	void addListener(Consumer<T> listener);
	void removeListener(Consumer<T> listener);
}
