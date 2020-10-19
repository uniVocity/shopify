package com.univocity.shopify.utils;

import org.apache.commons.lang3.*;
import org.slf4j.*;

import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.util.concurrent.*;

public class IpBlocker {

	private static final Logger log = LoggerFactory.getLogger(IpBlocker.class);

	private static final int COUNT = 0;
	private static final int LAST_HIT_TIME = 1;
	private static final int HIT_INTERVAL = 2;

	private final Set<String> loggedIps = Collections.synchronizedSet(new CircularSet<>(1_000));
	private final TimedCache<String, long[]> illegalAccessCount;

	public IpBlocker(int maxSize) {
		this.illegalAccessCount = new TimedCache<>(TimeUnit.HOURS.toMillis(12), maxSize);
		this.illegalAccessCount.setEvictionHandler(loggedIps::remove);
	}

	public static final String getIp(ServletRequest request) {
		HttpServletRequest servletRequest = (HttpServletRequest) request;
		String ip = servletRequest.getHeader("X-FORWARDED-FOR");
		if (ip == null) {
			ip = request.getRemoteAddr();
		}

		if (StringUtils.isBlank(ip)) {
			return null;
		}

		return ip;
	}

	public static final String getEndpoint(ServletRequest request) {
		HttpServletRequest servletRequest = (HttpServletRequest) request;
		String out = servletRequest.getRequestURI();
		if (StringUtils.isBlank(out)) {
			return "";
		}
		return out;
	}

	public void register(String ip) {
		if (ip != null) {
			long[] info = illegalAccessCount.get(ip);
			if (info == null) {
				info = new long[]{1, System.currentTimeMillis(), 0L};
				illegalAccessCount.put(ip, info);
			} else {
				long now = System.currentTimeMillis();
				synchronized (info) {
					long timeElapsed = now - info[LAST_HIT_TIME];
					info[LAST_HIT_TIME] = now;
					info[COUNT]++;
					info[HIT_INTERVAL] += timeElapsed;
				}
			}
		}
	}

	public boolean shouldBlock(String ip, long minHitInterval, String msg) {
		if (ip == null) {
			return true;
		}
		long[] info = illegalAccessCount.get(ip);
		if (info != null && info[HIT_INTERVAL] > 0L) {
			synchronized (info) {
				long average = info[HIT_INTERVAL] / info[COUNT];
				if (average < minHitInterval) {
					if(!loggedIps.contains(ip)) {
						log.info(msg + ". Hit count: {}. Hit interval: {}. Avg: {}. Min interval {}", ip, info[COUNT], info[HIT_INTERVAL], average, minHitInterval);
						loggedIps.add(ip);
					}
					return true;
				} else if(average > minHitInterval * 2L){ //clear to prevent having a large hit interval and allow short bursts of access as the average can get too large
					info[COUNT] = 0L;
					info[HIT_INTERVAL] = 0L;
				}
			}
		}
		return false;
	}
}
