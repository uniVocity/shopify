package com.univocity.shopify.model.shopify;

import com.fasterxml.jackson.annotation.*;

import java.util.*;

public class ProductList {
	@JsonProperty
	private List<ShopifyProduct> products;
}
