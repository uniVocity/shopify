package com.univocity.shopify.price;

import com.univocity.shopify.utils.*;
import org.slf4j.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * An aggregator of prices of for a given token/currency pair. Will query multiple sources
 * at once and return an average of the resulting price.
 */
public class PriceAggregator {
	private static final Logger log = LoggerFactory.getLogger(PriceAggregator.class);

	private static final long _30_SECONDS = Duration.ofSeconds(30).toMillis();
	private static final long _3_SECONDS = Duration.ofSeconds(3).toMillis();

	// TODO: use dependency injection later. For now just run off a main method for testing
	private BinancePriceConverter binance = new BinancePriceConverter();

	private final List<PriceConverter> converters = Arrays.asList(
			binance
			// TODO: add more converters to get prices from other sources
	);


	private final String tokenSymbol;
	private final String currencySymbol;


	private double count;
	private double sum;
	private double averagePrice = -1.0;
	private long timeSinceLastUpdate;

	private CountDownLatch countDownLatch;
	final private ExecutorService executor;

	public PriceAggregator(String tokenSymbol, String currencySymbol) {
		this.tokenSymbol = tokenSymbol;
		this.currencySymbol = currencySymbol;

		executor = Executors.newCachedThreadPool(new DaemonThreadFactory());
	}

	/**
	 * Returns the average price of the ticker being monitored by this aggregator,
	 * averaged from prices returned by multiple sources such as exchanges.
	 *
	 * Price updates will not execute more than once every 30 seconds.
	 *
	 * @return the latest ticker price.
	 */
	public double getPrice() {
		long now = System.currentTimeMillis();
		if (now - timeSinceLastUpdate > _30_SECONDS) {
			if (countDownLatch == null || countDownLatch.getCount() == 0) {
				synchronized (this) {
					sum = 0;
					count = 0;
				}
				countDownLatch = new CountDownLatch(converters.size());
				converters.forEach(c -> executor.submit(() -> compute(c)));
				timeSinceLastUpdate = now;
			}
		}

		while (countDownLatch.getCount() > 0 && count == 0) {
			try {
				countDownLatch.await(1, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				log.warn("Thread interrupted updating prices of" + tokenSymbol + " in " + currencySymbol, e);
				Thread.currentThread().interrupt();
				break;
			}
			if (System.currentTimeMillis() - now > _3_SECONDS) {
				break;
			}
		}

		synchronized (this) {
			if (count > 0) {
				averagePrice = sum / count;
			} else {
				log.warn("Could not compute average price of " + tokenSymbol + "/" + currencySymbol + ". Using previous calculated price of " + averagePrice);
			}
		}

		return averagePrice;
	}

	private void compute(PriceConverter converter) {
		try {
			double price = converter.getLatestPrice(tokenSymbol, currencySymbol);
			if (price != -1) {
				synchronized (this) {
					sum += price;
					count++;
				}
			} else {
				log.warn("No price available from " + converter.getClass().getSimpleName() + " for " + tokenSymbol + "/" + currencySymbol);
			}
		} finally {
			countDownLatch.countDown();
		}
	}

	public static void main(String... args) throws Exception {
		PriceAggregator aggregator = new PriceAggregator("ADA", "USDT");
		System.out.println(aggregator.getPrice());
		Thread.sleep(11000);
		System.out.println(aggregator.getPrice());
		Thread.sleep(11000);
		System.out.println(aggregator.getPrice());
		Thread.sleep(11000);

		//should be updated here:
		System.out.println(aggregator.getPrice());

		System.exit(0);

	}
}
