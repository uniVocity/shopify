package com.univocity.shopify.price;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CoinGeckoPriceConverter implements PriceConverter {
    private final String coinGeckcoBaseUrl = "https://api.coingecko.com/api/v3/";
    private final String[] urlParameters = {"cardano", "usd"};
    HttpURLConnection connection = null;

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
            String jsonCardanoResponse = response.body();
            String delimitedJsonCardanoResponse = jsonCardanoResponse.replaceAll("[^0-9]","");
            delimitedJsonCardanoResponse.trim();
            String formatJsonCardanoResponse = "0." + delimitedJsonCardanoResponse;

            return Double.parseDouble(formatJsonCardanoResponse);

        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
