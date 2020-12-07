package com.univocity.shopify.price;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PriceAggregatorTest {
    //TODO make a more robust testing suite for the Cardano price APIs

    @Test
    @DisplayName("Test to make sure we get all prices from the multiple price sources we call on")
    void getPrice() {
        //initialize all exchange/ price sources here
        final BinancePriceConverter binancePriceConverter = new BinancePriceConverter();
        final CoinGeckoPriceConverter coinGeckoPriceConverter = new CoinGeckoPriceConverter();
        final HitBtcPriceConverter hitBtcPriceConverter = new HitBtcPriceConverter();

        //Test all data sources here
        assertTrue(binancePriceConverter.getLatestPrice("ADA", "USDT") > 0);
        assertTrue(coinGeckoPriceConverter.getLatestPrice("ADA", "USDT") > 0);
        assertTrue(hitBtcPriceConverter.getLatestPrice("ADA", "USDT") > 0);

    }
}