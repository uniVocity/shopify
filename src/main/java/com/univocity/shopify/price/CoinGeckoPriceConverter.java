package com.univocity.shopify.price;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


public class CoinGeckoPriceConverter implements PriceConverter {

    @Override
    public double getLatestPrice(String tokenSymbol, String currencySymbol) {
        if ((tokenSymbol.equals("ADA")) && (currencySymbol.equals("USDT"))) {
            return executeGetCardanoPrice();
        }
        return -1;
    }

    private double executeGetCardanoPrice() {
        try {
            HttpClient client = HttpClient.newHttpClient();

            // create a request
            HttpRequest request = HttpRequest.newBuilder(
                    URI.create("https://api.coingecko.com/api/v3/simple/price?ids=cardano&vs_currencies=usd"))
                    .header("accept", "application/json")
                    .build();

            // use the client to send the request
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            //Get the body of the Json response - In this case the price of cardano
            String jsonCardanoResponse = response.body();

            //FIXME The next three lines of code will have to be refactored. If the price of Cardano goes above 1 dollar,
            // This method of delimiting the json body will mess it up. Will need to implement a Json Object/Model
            // To map the json response of the Cardano price to the Model/Json Object Price.
            String delimitedJsonCardanoResponse = jsonCardanoResponse.replaceAll("[^0-9]", "");
            String formatJsonCardanoResponse = "0." + delimitedJsonCardanoResponse;

            return Double.parseDouble(formatJsonCardanoResponse);

        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
}

