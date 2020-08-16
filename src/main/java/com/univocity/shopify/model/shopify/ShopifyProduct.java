package com.univocity.shopify.model.shopify;

import com.fasterxml.jackson.annotation.*;

import java.util.*;

public class ShopifyProduct {
	@JsonProperty
	public long id;

	@JsonProperty
	public String title;

	@JsonProperty
	public ShopifyImage image;

	@JsonProperty
	public List<ShopifyImage> images;

	@JsonProperty()
	public List<ProductVariant> variants;

	@Override
	public String toString() {
		return "ShopifyProduct{" +
				"id=" + id +
				", title='" + title + '\'' +
				", image=" + image +
				", images=" + images +
				", variants=" + variants +
				'}';
	}
}
