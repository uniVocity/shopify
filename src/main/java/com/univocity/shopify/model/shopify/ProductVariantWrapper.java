package com.univocity.shopify.model.shopify;

import com.fasterxml.jackson.annotation.*;

public class ProductVariantWrapper {

	@JsonProperty
	public ProductVariant variant;

	@Override
	public String toString() {
		return "ProductVariantWrapper{" +
				"variant=" + variant +
				'}';
	}
}
