package com.univocity.shopify.model;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.Date;

public class Instrument {

    @JsonAlias({"Crypto_Name"})
    public String CryptoCurrencyName;

    @JsonAlias({"symbol", "ticker"})
    public String symbol;

    @JsonAlias({"ask"})
    public String ask;

    @JsonAlias({"bid"})
    public String bid;

    @JsonAlias({"last"})
    public String last;

    @JsonAlias({"low"})
    public String low;

    @JsonAlias({"high"})
    public String high;

    @JsonAlias({"open"})
    public String open;

    @JsonAlias({"volume"})
    public String volume;

    @JsonAlias({"volumeQuote"})
    public String volumeQuote;

    @JsonAlias({"timestamp"})
    public Date timestamp;
}
