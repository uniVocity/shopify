package com.univocity.shopify.model.shopify;

import com.fasterxml.jackson.annotation.*;

public class DataRequest {

	@JsonProperty
	public long shop_id;

	@JsonProperty
	public String shop_domain;

	@JsonProperty
	public ShopifyCustomer customer;

	@JsonProperty
	public Long[] orders_requested;
}
