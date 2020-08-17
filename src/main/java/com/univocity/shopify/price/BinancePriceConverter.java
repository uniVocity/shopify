package com.univocity.shopify.price;

import com.binance.api.client.*;
import com.binance.api.client.domain.market.*;
import org.slf4j.*;

class BinancePriceConverter implements PriceConverter {

	private static final Logger log = LoggerFactory.getLogger(BinancePriceConverter.class);

	private static final BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance();

	private BinanceApiRestClient client;

	@Override
	public double getLatestPrice(String tokenSymbol, String currencySymbol) {
		String ticker = tokenSymbol + currencySymbol;

		try {
			TickerPrice tickerPrice = client().getPrice(ticker.toUpperCase());
			String price = tickerPrice.getPrice();
			return Double.parseDouble(price);
		} catch (Exception e) {
			log.warn("Unable to obtain price of " + ticker + " from Binance", e);
			client = null; // just in case, reset the client
			return -1.0;
		}
	}

	private BinanceApiRestClient client() {
		if (client == null) {
			client = factory.newRestClient();
		}
		return client;
	}


	// just a quick and dirty check to see if it works
	public static void main(String... args) {
		System.out.println(new BinancePriceConverter().getLatestPrice("ADA", "USDT"));
	}
}