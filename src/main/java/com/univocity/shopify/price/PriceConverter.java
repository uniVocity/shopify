package com.univocity.shopify.price;

/**
 * A converter of token prices. Used to determine the latest price of a token
 */
public interface PriceConverter {

	/**
	 * Returns the current token price for a given currency symbol.
	 *
	 * @param tokenSymbol    the symbol of the token whose price will be queried (e.g. ADA)
	 * @param currencySymbol the symbol of the currency or token that represents the price unit (e.g. USDT, EUR, BTC)
	 */
	double getLatestPrice(String tokenSymbol, String currencySymbol);
}