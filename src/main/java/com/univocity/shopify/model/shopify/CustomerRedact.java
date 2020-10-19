package com.univocity.shopify.model.shopify;

import com.fasterxml.jackson.annotation.*;

public class CustomerRedact {

	@JsonProperty
	public long shop_id;

	@JsonProperty
	public String shop_domain;

	@JsonProperty
	public ShopifyCustomer customer;

	@JsonProperty
	public Long[] orders_to_redact;
}
