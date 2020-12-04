package com.univocity.shopify.price;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriceAggregatorTest {

    @Test
    @DisplayName("Test to make sure we get all prices from the multiple price sources we call on")
    void getPrice() {
        //initialize all exchange/ price sources here
        final BinancePriceConverter binancePriceConverter = new BinancePriceConverter();

        //Test all data sources here
        Assertions.assertAll(() -> assertTrue(binancePriceConverter.getLatestPrice("ADA","USDT") > 0));
    }
}