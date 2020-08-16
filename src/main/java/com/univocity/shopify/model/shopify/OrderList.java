package com.univocity.shopify.model.shopify;

import com.fasterxml.jackson.annotation.*;

import java.util.*;

public class OrderList {
	@JsonProperty
	public List<ShopifyOrder> orders;

	public OrderList() {

	}

	@Override
	public String toString() {
		return "OrderList{" +
				"orders=" + orders +
				'}';
	}
}
