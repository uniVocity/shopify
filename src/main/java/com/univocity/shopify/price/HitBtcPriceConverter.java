package com.univocity.shopify.price;

public class HitBtcPriceConverter implements PriceConverter{
    @Override
    public double getLatestPrice(String tokenSymbol, String currencySymbol) {
        return 0;
        //https://api.hitbtc.com/api/2/public/ticker/ADAUSD
    }
}
