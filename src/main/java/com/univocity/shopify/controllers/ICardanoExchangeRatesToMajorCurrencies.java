package com.univocity.shopify.controllers;

import java.text.NumberFormat;

public interface ICardanoExchangeRatesToMajorCurrencies {
    //Helper method incase you want to format what is being returned. Takes
    // in the price of ADA as a double, and returns a currncey format. Example
    // NumberFormatter(2.22) returns $2.22
    static String NumberFormatter(double numberToFormat) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance();
        return formatter.format(numberToFormat);

    }

    //
    double GetCardanoPriceInUSD();

    double GetCardanoPriceInGBP();

    double GetCardanoPriceInEUR();

    double GetCardanoPriceInAUD();
}
