package com.univocity.shopify.price;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.univocity.shopify.model.Instrument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HitBtcPriceConverter implements PriceConverter {

    private static final Logger log = LoggerFactory.getLogger(BinancePriceConverter.class);
    private static final HttpClient client = HttpClient.newHttpClient();
    private final String baseHitBtcUrl = "https://api.hitbtc.com/api/2/public/";

    @Override
    public double getLatestPrice(String tokenSymbol, String currencySymbol) {
        return executeGetCardanoPrice();
    }

    private double executeGetCardanoPrice() {
        try {
            // This is the price endpoint to get the price of Cardano with arguments included
            String hitBtcPriceQueryEndPoint = "ticker/ADAUSD";

            // create a request to HitBTC for a response of bid, ask, last, etc For Cardano
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
            log.warn("Unable to obtain price of Cardano from Hit BTC", e);
            return -1;
        }
    }
}
