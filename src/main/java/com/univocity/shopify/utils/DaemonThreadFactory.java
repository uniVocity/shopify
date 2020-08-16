package com.univocity.shopify.utils;

import java.util.concurrent.*;

/**
 * A simple thread factory for creating daemon threads
 */
public class DaemonThreadFactory implements ThreadFactory {
	public Thread newThread(Runnable r) {
		Thread t = new Thread(r);
		t.setDaemon(true);
		return t;
	}
}