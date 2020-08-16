package com.univocity.shopify.model.shopify;

import com.fasterxml.jackson.annotation.*;

/**
 * @author uniVocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 */
public class ProductWrapper {

	@JsonProperty
	public ShopifyProduct product;

	@Override
	public String toString() {
		return "ProductWrapper{" +
				"product=" + product +
				'}';
	}
}
