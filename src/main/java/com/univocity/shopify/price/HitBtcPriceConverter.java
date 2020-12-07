package com.univocity.shopify.price;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.univocity.shopify.model.Instrument;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HitBtcPriceConverter implements PriceConverter{

    private final String baseHitBtcUrl = "https://api.hitbtc.com/api/2/public/";

    @Override
    public double getLatestPrice(String tokenSymbol, String currencySymbol) {
        return executeGetCardanoPrice();
    }

    private double executeGetCardanoPrice() {
        try {
            String hitBtcPriceQueryEndPoint = "ticker/ADAUSD";
            HttpClient client = HttpClient.newHttpClient();

            // create a request
            HttpRequest request = HttpRequest.newBuilder(
                    URI.create(baseHitBtcUrl + hitBtcPriceQueryEndPoint))
                    .header("accept", "application/json")
                    .build();

            // use the client to send the request
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            //Get the body of the Json response - In this case the price of cardano
            String jsonCardanoResponse = response.body();

            //Next two lines maps the json response to the Instrument model java object
            ObjectMapper om = new ObjectMapper();
            Instrument cardanoInstrument = om.readValue(jsonCardanoResponse, Instrument.class);

            return Double.parseDouble(cardanoInstrument.last);

        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
}
