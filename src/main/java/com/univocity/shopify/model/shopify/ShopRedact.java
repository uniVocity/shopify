package com.univocity.shopify.model.shopify;

import com.fasterxml.jackson.annotation.*;

public class ShopRedact {
	@JsonProperty
	public long shop_id;

	@JsonProperty
	public String shop_domain;
}
