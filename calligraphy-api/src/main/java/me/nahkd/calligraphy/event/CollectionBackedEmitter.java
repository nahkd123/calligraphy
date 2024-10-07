package me.nahkd.calligraphy.event;

import java.util.Collection;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * <p>
 * Collection-backed emitter: This uses {@link Collection} to store listeners. Recommend using {@link Set} for
 * performance.
 * </p>
 * @param <T>
 */
public class CollectionBackedEmitter<T> implements Emitter<T> {
	private Collection<Consumer<T>> listeners;
	private Collection<Consumer<T>> pendingAdd;
	private Collection<Consumer<T>> pendingRemove;
	private Queue<T> emitQueue = new ConcurrentLinkedQueue<>();
	private boolean emitting = false;

	public CollectionBackedEmitter(Supplier<? extends Collection<Consumer<T>>> factory) {
		this.listeners = factory.get();
		this.pendingAdd = factory.get();
		this.pendingRemove = factory.get();
	}

	public CollectionBackedEmitter() {
		this(HashSet::new);
	}

	@Override
	public void addListener(Consumer<T> listener) {
		if (!emitting) {
			listeners.add(listener);
			return;
		}

		if (pendingRemove.contains(listener)) {
			pendingRemove.remove(listener);
			return;
		}

		if (!pendingAdd.contains(listener)) {
			pendingAdd.add(listener);
			return;
		}
	}

	@Override
	public void removeListener(Consumer<T> listener) {
		if (!emitting) {
			listeners.remove(listener);
			return;
		}

		if (pendingAdd.contains(listener)) {
			pendingAdd.remove(listener);
			return;
		}

		if (!pendingRemove.contains(listener)) {
			pendingRemove.add(listener);
			return;
		}
	}

	public void emit(T obj) {
		emitQueue.add(obj);
		if (emitting) return;
		emitting = true;

		try {
			while (!emitQueue.isEmpty()) {
				T event = emitQueue.poll();

				for (Consumer<T> listener : listeners) {
					try {
						listener.accept(event);
					} catch (Exception e) {
						// TODO
						e.printStackTrace();
					}
				}

				if (pendingAdd.size() > 0) {
					pendingAdd.forEach(listeners::add);
					pendingAdd.clear();
				}

				if (pendingRemove.size() > 0) {
					pendingRemove.forEach(listeners::remove);
					pendingRemove.clear();
				}
			}
		} finally {
			emitting = false;
		}
	}
}
