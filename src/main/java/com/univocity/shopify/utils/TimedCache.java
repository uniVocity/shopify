

package com.univocity.shopify.utils;


import java.lang.ref.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

public class TimedCache<K, V> {

	private static final long EIGHT_HOURS = TimeUnit.HOURS.toMillis(8L);

	private final ConcurrentHashMap<K, SoftReference<Object[]>> cache;

	private long timeSinceLastCleanup = 0L;

	private final long entryTimeToLive;
	private final int maxLoad;
	private float evictionFactor;
	private final Lock cleanupLock = new ReentrantLock();
	private boolean cleanupActive = false;
	private EvictionHandler<K> evictionHandler;
	private final ExecutorService executorService = Executors.newSingleThreadExecutor(new DaemonThreadFactory());

	public interface EvictionHandler<K> {
		void keyExpired(K key);
	}

	public TimedCache() {
		this(EIGHT_HOURS);
	}

	public TimedCache(long entryTimeToLive) {
		this(entryTimeToLive, 2500000);
	}

	public TimedCache(long entryTimeToLive, int maxLoad) {
		this(entryTimeToLive, maxLoad, 0.75f);
	}

	public TimedCache(long entryTimeToLive, int maxLoad, float evictionFactor) {
		Utils.positive((int) entryTimeToLive, "Entry time-to-live");
		Utils.positive(maxLoad, "Maximum load");
		setEvictionFactor(evictionFactor);

		cache = new ConcurrentHashMap<K, SoftReference<Object[]>>();
		this.entryTimeToLive = entryTimeToLive;

		this.maxLoad = maxLoad;

	}

	private class CleanupProcess implements Runnable {

		final int maxSize;

		CleanupProcess() {
			maxSize = (int) (maxLoad * evictionFactor);
		}

		@Override
		public void run() {
			final long time = System.currentTimeMillis();
			try {
				Iterator<Map.Entry<K, SoftReference<Object[]>>> it = cache.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry<K, SoftReference<Object[]>> entry = it.next();

					SoftReference<Object[]> r = entry.getValue();
					if (r == null) {
						evict(entry, it);
						continue;
					}
					Object[] v = r.get();
					if (v == null) {
						evict(entry, it);
						continue;
					}

					Long entryTime = (Long) v[0];

					if (time - entryTime > entryTimeToLive || size() > maxLoad) {
						evict(entry, it);
					}
				}
			} finally {
				timeSinceLastCleanup = time;
				cleanupActive = false;
			}
		}
	}

	private final CleanupProcess cleanupProcess = new CleanupProcess();

	public float getEvictionFactor() {
		return evictionFactor;
	}

	public void setEvictionFactor(float evictionFactor) {
		if (evictionFactor > 1.0f || evictionFactor < 0.0f) {
			throw new IllegalArgumentException("Eviction factor must be between 0.0 and 1.0");
		}
		this.evictionFactor = evictionFactor;
	}

	public int size() {
		return cache.size();
	}

	public void put(K key, V value) {
		Object[] entry = new Object[]{System.currentTimeMillis(), value};
		SoftReference<Object[]> ref = new SoftReference<Object[]>(entry);
		cache.put(key, ref);
		cleanup();
	}

	private void cleanup() {
		if (cleanupActive) {
			return;
		}
		if (cleanupLock.tryLock()) {
			if (cleanupActive) {
				return;
			}
			try {
				final long time = System.currentTimeMillis();

				if (timeSinceLastCleanup == 0) {
					timeSinceLastCleanup = time;
				}

				int size = size();
				if (size > maxLoad || time - timeSinceLastCleanup > entryTimeToLive) {
					cleanupActive = true;
					executorService.submit(cleanupProcess);
				}
			} finally {
				cleanupLock.unlock();
			}
		}
	}

	public void clear() {
		cache.clear();
	}

	public V remove(K key) {
		return unwrap(key, cache.remove(key));
	}

	public V get(K key) {
		V out;
		out = unwrap(key, cache.get(key));
		cleanup();
		return out;
	}

	private V unwrap(K key, SoftReference<Object[]> r) {
		if (r != null) {
			Object[] out = r.get();
			if (out != null) {
				Long entryTime = (Long) out[0];

				if (System.currentTimeMillis() - entryTime <= entryTimeToLive) {
					return (V) out[1];
				}
			}
		}
		evict(key);
		return null;
	}

	private void evict(K key) {
		cache.remove(key);
		if (evictionHandler != null) {
			evictionHandler.keyExpired(key);
		}
	}

	private void evict(Map.Entry<K, SoftReference<Object[]>> entry, Iterator<Map.Entry<K, SoftReference<Object[]>>> it){
		K key = entry.getKey();
		it.remove();
		if (evictionHandler != null) {
			evictionHandler.keyExpired(key);
		}
	}

	public void setEvictionHandler(EvictionHandler<K> evictionHandler) {
		this.evictionHandler = evictionHandler;
	}
}
