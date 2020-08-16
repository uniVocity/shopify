

package com.univocity.shopify.model.shopify;

import com.fasterxml.jackson.annotation.*;

public class ImageWrapper {

	@JsonProperty
	public ShopifyImage image;

	@Override
	public String toString() {
		return "ImageWrapper{" +
				"image=" + image +
				'}';
	}
}
