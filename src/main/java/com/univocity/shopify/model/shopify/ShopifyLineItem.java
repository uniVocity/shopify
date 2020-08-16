package com.univocity.shopify.model.shopify;

import com.fasterxml.jackson.annotation.*;

import java.math.*;

public class ShopifyLineItem {

	@JsonProperty
	public Long id;

	public Long orderId;

	@JsonProperty("product_id")
	public Long productId;

	@JsonProperty("variant_id")
	public Long variantId;

	@JsonProperty
	public String title;

	@JsonProperty
	public String name;

	@JsonProperty(value = "sku")
	public String sku;

	@JsonProperty(value = "variant_title")
	public String variantTitle;

	@JsonProperty
	public Long quantity;

	@JsonProperty
	public BigDecimal price;

	public ShopifyLineItem() {

	}

	public Long getProductId(){
		return productId;
	}

	@Override
	public String toString() {
		return "LineItem{" +
				"id=" + id +
				", orderId=" + orderId +
				", productId=" + productId +
				", variantId=" + variantId +
				", title='" + title + '\'' +
				", name='" + name + '\'' +
				", sku='" + sku + '\'' +
				", variantTitle='" + variantTitle + '\'' +
				", quantity=" + quantity +
				", price=" + price +
				'}';
	}
}
