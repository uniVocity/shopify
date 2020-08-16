package com.univocity.shopify.utils;

import org.apache.commons.lang3.*;
import org.slf4j.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * A very simple method call rate limiter, based on a time interval. Users can call {@link #waitAndGo()}
 * or {@link #waitAndGo(long)} to block the current thread before performing an operation that is potentially costly
 * on other resources (such as HTTP requests to remote servers).
 */
public class RateLimiter {

	private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);

	private long lastCallTime;
	private long waitTimeAdjustment;

	private long interval;
	private final Object callLock = new Object();
	private final AtomicInteger callWaitingCount = new AtomicInteger();
	private final String name;

	/**
	 * Creates a new rate limiter configured to allow for a given interval of time
	 * to elapse before allowing a process to continue.
	 *
	 * @param interval the interval to wait between each call to {@link #waitAndGo()}
	 */
	public RateLimiter(long interval) {
		this(null, interval);
	}

	/**
	 * Creates a new rate limiter configured to allow for a given interval of time
	 * to elapse before allowing a process to continue.
	 *
	 * @param name a name for the rate limiter, used for logging
	 * @param interval the interval to wait between each call to {@link #waitAndGo()}
	 */
	public RateLimiter(String name, long interval) {
		Utils.positiveOrZero(interval, "Interval");
		this.interval = interval;
		this.name = StringUtils.isBlank(name) ? "" : name.trim();
	}

	/**
	 * Returns the name associated with this rate limiter, used for logging.
	 * @return the name of this rate limiter
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the current rate interval
	 * @return the current interval
	 */
	public final long getInterval() {
		return interval;
	}

	/**
	 * Modifies the rate interval
	 * @param interval the new interval
	 */
	public void setInterval(long interval) {
		Utils.positiveOrZero(interval, "Interval");
		this.interval = interval;
		if (interval == 0L) {
			synchronized (callLock) {
				callLock.notifyAll();
			}
		}
	}


	/**
	 * Returns the number of threads blocked and waiting for their turn to execute
	 * @return the number of threads blocked
	 */
	public final long getWaitingCount() {
		return callWaitingCount.get();
	}

	/**
	 * Blocks the current process if the previous call to this method happened at a time less than the configured
	 * interval. The waiting time will be the time difference between the configured interval and the time elapsed
	 * since the previous call to this method.
	 *
	 * @param timeout the maximum length of time the process is allowed to wait. A {@code TimeoutException} will
	 *                be thrown if the process got locked up for too long.
	 *
	 *
	 * @return the number of threads still blocked and waiting.
	 *
	 * @throws TimeoutException if the wait time exceeds the given timeout. Will only be thrown after the process
	 * gets unblocked - at some time potentially much longer than the given timeout.
	 */
	public final long waitAndGo(long timeout) throws TimeoutException {
		return waitAndGo("operation", timeout);
	}

	/**
	 * Blocks the current process if the previous call to this method happened at a time less than the configured
	 * interval. The waiting time will be the time difference between the configured interval and the time elapsed
	 * since the previous call to this method.
	 *
	 * @param action description of the action to be performed after the wait time is over. Used for logging.
	 *
	 * @param timeout the maximum length of time the process is allowed to wait. A {@code TimeoutException} will
	 *                be thrown if the process got locked up for too long.
	 *
	 *
	 * @return the number of threads still blocked and waiting.
	 *
	 * @throws TimeoutException if the wait time exceeds the given timeout. Will only be thrown after the process
	 * gets unblocked - at some time potentially much longer than the given timeout.
	 */
	public final long waitAndGo(String action, long timeout) throws TimeoutException {
		long start = 0L;
		if (timeout > 0) {
			start = System.currentTimeMillis();
		}
		callWaitingCount.incrementAndGet();
		synchronized (callLock) {
			long time = System.currentTimeMillis();
			if (timeout > 0 && time - start > timeout) {
				throw new TimeoutException(name + " rate limiter: " + action + " timed out after " + (time - start) + "ms");
			}
			doWait(time, action);
		}
		return callWaitingCount.decrementAndGet();
	}

	/**
	 * Blocks the current process if the previous call to this method happened at a time less than the configured
	 * interval. The waiting time will be the time difference between the configured interval and the time elapsed
	 * since the previous call to this method.
	 *
	 * @return the number of threads still blocked and waiting.
	 */
	public final long waitAndGo() {
		return waitAndGo("proceeding");
	}

	/**
	 * Blocks the current process if the previous call to this method happened at a time less than the configured
	 * interval. The waiting time will be the time difference between the configured interval and the time elapsed
	 * since the previous call to this method.
	 *
	 * @param action description of the action to be performed after the wait time is over. Used for logging.
	 *
	 * @return the number of threads still blocked and waiting.
	 */
	public final long waitAndGo(String action) {
		callWaitingCount.incrementAndGet();
		synchronized (callLock) {
			doWait(System.currentTimeMillis(), action);
		}
		return callWaitingCount.decrementAndGet();
	}

	private void doWait(long currentTime, String action) {
		long waitTime = lastCallTime == 0 ? 0 : (interval + waitTimeAdjustment) - (currentTime - lastCallTime);
		waitTimeAdjustment = 0L;

		if (waitTime > 0L) {
			try {
				log.debug("{} rate limiter active: waiting {} ms before {}", name, waitTime, action);
				Thread.sleep(waitTime);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		lastCallTime = System.currentTimeMillis();
	}

	/**
	 * Decreases the wait time of the <em>next</em> process that will be put to wait.
	 * The wait time returns back to the interval provided in the constructor of this
	 * class after the next process is blocked
	 *
	 * @param timeToDecrease length of time to decrease from the configured interval.
	 */
	public final void decreaseWaitTime(long timeToDecrease) {
		Utils.positive(timeToDecrease, "Time to decrease");
		waitTimeAdjustment = -timeToDecrease;
	}

	/**
	 * Increases the wait time of the <em>next</em> process that will be put to wait.
	 * The wait time returns back to the interval provided in the constructor of this
	 * class after the next process is blocked
	 *
	 * @param timeToIncrease length of time add to the configured interval.
	 */
	public final void increaseWaitTime(long timeToIncrease) {
		Utils.positive(timeToIncrease, "Time to increase");
		waitTimeAdjustment = timeToIncrease;
	}
}
