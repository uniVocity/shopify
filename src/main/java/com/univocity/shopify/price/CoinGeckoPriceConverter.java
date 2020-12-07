package com.univocity.shopify.price;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


public class CoinGeckoPriceConverter implements PriceConverter {


    private static final Logger log = LoggerFactory.getLogger(BinancePriceConverter.class);
    private static final HttpClient client = HttpClient.newHttpClient();
    private final String baseCoinGeckoUrl = "https://api.coingecko.com/api/v3/";

    @Override
    public double getLatestPrice(String tokenSymbol, String currencySymbol) {
        if ((tokenSymbol.equals("ADA")) && (currencySymbol.equals("USDT"))) {
            return executeGetCardanoPrice();
        }
        return -1;
    }

    private double executeGetCardanoPrice() {
        try {
            // This is the price endpoint to get the price of Cardano with arguments included
            String coinGeckoPriceQueryEndPoint = "simple/price?ids=cardano&vs_currencies=usd";

            // create a request to Coin Gecko for a purse price response.
            HttpRequest request = HttpRequest.newBuilder(
                    URI.create(baseCoinGeckoUrl + coinGeckoPriceQueryEndPoint))
                    .header("accept", "application/json")
                    .build();

            // use the client to send the request
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            //Get the body of the Json response - In this case bid, ask, volume, timestamp, last, etc.
            String jsonCardanoResponse = response.body();

            //FIXME The next three lines of code will have to be refactored. If the price of Cardano goes above 1 dollar,
            // This method of delimiting the json body will mess it up. Will need to implement a Json Object/Model
            // To map the json response of the Cardano price to the Model/Json Object Price.
            // I tried to use Jackson Object Mapper but it was not having it, so this is a to be continued.
            String delimitedJsonCardanoResponse = jsonCardanoResponse.replaceAll("[^0-9]", "");
            String formatJsonCardanoResponse = "0." + delimitedJsonCardanoResponse;

            return Double.parseDouble(formatJsonCardanoResponse);

        } catch (Exception e) {
            log.warn("Unable to obtain price of Cardano from Coin Gecko", e);
            return -1;
        }
    }
}

