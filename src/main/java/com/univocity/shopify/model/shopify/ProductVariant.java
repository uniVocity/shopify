package com.univocity.shopify.model.shopify;

import com.fasterxml.jackson.annotation.*;

import java.math.*;

public class ProductVariant {

	@JsonProperty
	public long id;

	@JsonProperty("product_id")
	public long productId;

	@JsonProperty
	public String title;

	@JsonProperty
	public String sku;

	@JsonProperty
	public BigDecimal price;

	@JsonProperty("image_id")
	public Long imageId;

	@Override
	public String toString() {
		return "ProductVariant{" +
				"id=" + id +
				", productId=" + productId +
				", title='" + title + '\'' +
				", sku='" + sku + '\'' +
				", price=" + price +
				", imageId=" + imageId +
				'}';
	}
}
