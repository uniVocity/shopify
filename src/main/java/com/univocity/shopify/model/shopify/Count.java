package com.univocity.shopify.model.shopify;

import com.fasterxml.jackson.annotation.*;

@JsonRootName("count")
public class Count {
	@JsonProperty
	public int count;

	public Count() {
	}

	public Count(int count) {
		this.count = count;
	}

	@Override
	public String toString() {
		return "Count{" +
				"count=" + count +
				'}';
	}
}
