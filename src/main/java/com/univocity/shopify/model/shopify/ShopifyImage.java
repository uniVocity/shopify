package com.univocity.shopify.model.shopify;

import com.fasterxml.jackson.annotation.*;

public class ShopifyImage {

	@JsonProperty
	public long id;

	@JsonProperty
	public String src;

	@JsonProperty
	public Integer width;

	@JsonProperty
	public Integer height;

	@Override
	public String toString() {
		return "ShopifyImage{" +
				"id=" + id +
				", src='" + src + '\'' +
				", width=" + width +
				", height=" + height +
				'}';
	}
}
