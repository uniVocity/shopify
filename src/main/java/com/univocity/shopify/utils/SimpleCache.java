package com.univocity.shopify.utils;

import java.lang.ref.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * @author Univocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 */
public class SimpleCache<V> {
	private static final Object NULL = new Object();

	public static final class Key {
		private final Object parent;
		private final Object[] keyValues;

		private Key(Object parent, Object[] keyValues) {
			this.parent = parent == null ? NULL : parent;
			this.keyValues = keyValues;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			Key key = (Key) o;
			return parent.equals(key.parent) && Arrays.equals(keyValues, key.keyValues);
		}

		@Override
		public int hashCode() {
			return 31 * (parent != NULL ? parent.hashCode() : 0) + Arrays.hashCode(keyValues);
		}

		@Override
		public String toString() {
			if (parent != null) {
				return parent + Arrays.toString(keyValues);
			} else {
				return Arrays.toString(keyValues);
			}
		}
	}


	private final ConcurrentHashMap<Key, SoftReference<Object>> cache = new ConcurrentHashMap<Key, SoftReference<Object>>();
	private final Set<Key> keySequence;

	private final int maxLoad;
	private final int evictionLoad;

	private final Object lock = new Object();

	public SimpleCache(int maxLoad, float evictionFactor) {
		this.maxLoad = maxLoad;
		this.evictionLoad = (int) (maxLoad * evictionFactor);
		this.keySequence = Collections.synchronizedSet(new LinkedHashSet<Key>(maxLoad));
	}

	public SimpleCache(int maxLoad) {
		this(maxLoad, 0.75f);
	}

	public SimpleCache() {
		this(4096);
	}

	public int size() {
		return cache.size();
	}

	private void makeRoom() {
		if (keySequence.size() >= maxLoad) {
			evict();
		}
	}

	private void evict() {
		synchronized (lock) {
			if (keySequence.size() >= maxLoad) {
				int removeCount = keySequence.size() - evictionLoad;
				for (Iterator<Key> it = keySequence.iterator(); it.hasNext() && removeCount > 0; removeCount--) {
					Key key = it.next();
					cache.remove(key);
					it.remove();
				}
			}
		}
	}

	public void put(Object[] keyValues, V value) {
		put(null, keyValues, value);
	}

	public void put(Object parent, Object[] keyValues, V value) {
		put(new Key(parent, keyValues), value);
	}

	private void put(Key key, Object value) {
		keySequence.remove(key);
		makeRoom();

		if (value == null) {
			value = NULL;
		}
		synchronized (lock) {
			cache.put(key, new SoftReference<Object>(value));
			keySequence.add(key);
		}
	}

	public void clear() {
		synchronized (lock) {
			cache.clear();
			keySequence.clear();
		}
	}

	public V remove(Object[] keyValues) {
		return remove(null, keyValues);
	}

	public V remove(Object parent, Object[] keyValues) {
		Key key = new Key(parent, keyValues);
		return remove(key);
	}

	public V remove(Key key) {
		SoftReference<Object> value = cache.remove(key);
		keySequence.remove(key);

		if (value == null || value.get() == NULL) {
			return null;
		}
		return (V) value.get();
	}

	public V get(Object[] keyValues) {
		return get(null, keyValues);
	}

	public V get(Object parent, Object[] keyValues) {
		return get(parent, keyValues, false, null);
	}

	public V get(Object[] keyValues, Function<Object[], V> Function) {
		return get(null, keyValues, Function);
	}

	public V get(Object parent, Object[] keyValues, Function<Object[], V> Function) {
		return get(parent, keyValues, false, Function);
	}

	public V get(Object[] keyValues, boolean retryNull, Function<Object[], V> Function) {
		return get(null, keyValues, retryNull, Function);
	}

	public V get(Object parent, Object[] keyValues, boolean retryNull, Function<Object[], V> Function) {
		Key key = new Key(parent, keyValues);
		return get(key, retryNull, Function);
	}

	public V get(Key key) {
		return get(key, false, null);
	}

	private V get(Key key, boolean retryNull, Function<Object[], V> Function) {
		SoftReference<Object> value = cache.get(key);
		Object out;
		if (value == null || value.get() == null || (value.get() == NULL && retryNull)) {
			if (Function == null) {
				return null;
			}
			out = Function.apply(key.keyValues);
			put(key, out);
		} else {
			out = value.get();
		}
		return out == NULL ? null : (V) out;
	}

	public Set<Key> keys() {
		return Collections.unmodifiableSet(cache.keySet());
	}
}
