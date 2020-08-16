package com.univocity.shopify.controllers;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.market.TickerPrice;
import com.binance.api.client.exception.BinanceApiException;

import java.util.logging.Logger;


public class QueryADAPriceAgainstMajorCurrencies implements ICardanoExchangeRatesToMajorCurrencies {
    private final BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance();
    private final BinanceApiRestClient client = factory.newRestClient();

    //Queries the Binance Exchange using the Java Binance API. Gets the current Cardano Price
    //and the method then converts it and returns a useable double. 
    @Override
    public double GetCardanoPriceInUSD() {
        String cardanoPrice;
        try {
            TickerPrice cardanoTickerPrice = client.getPrice("ADA");
            cardanoPrice = cardanoTickerPrice.getPrice();
            return Double.parseDouble(cardanoPrice);
        } catch (BinanceApiException e) {
            client.ping();
            long response = client.getServerTime();
            if (response == 0L) {
                throw new TypeNotPresentException("CardanoValue",e.getCause());
            }
            e.getMessage();

        } catch (Exception e) {
            e.getMessage();
        } finally {
            return 0.0;
        }

    }

    @Override
    public double GetCardanoPriceInGBP() {
        return 0.0;
    }

    @Override
    public double GetCardanoPriceInEUR() {
        return 0.0;
    }

    @Override
    public double GetCardanoPriceInAUD() {
        return 0.0;
    }

}