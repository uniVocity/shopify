package com.univocity.shopify.model.shopify;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.*;
import com.univocity.shopify.model.shopify.serializer.*;

import java.time.*;
import java.util.*;

public class ShopifyCustomer {

	@JsonProperty
	public Long id;


	@JsonProperty
	public String email;

	@JsonProperty(value = "first_name")
	public String firstName;

	@JsonProperty(value = "last_name")
	public String lastName;

	@JsonProperty("created_at")
	@JsonDeserialize(using = ShopifyDateDeserializer.class)
	@JsonSerialize(using = ShopifyDateSerializer.class)
	public ZonedDateTime createdAt;

	@JsonProperty("updated_at")
	@JsonDeserialize(using = ShopifyDateDeserializer.class)
	@JsonSerialize(using = ShopifyDateSerializer.class)
	public ZonedDateTime updatedAt;


	public ShopifyCustomer() {

	}

	@Override
	public String toString() {
		return "ShopifyCustomer{" +
				"id=" + id +
				", email='" + email + '\'' +
				", firstName='" + firstName + '\'' +
				", lastName='" + lastName + '\'' +
				", createdAt=" + createdAt +
				", updatedAt=" + updatedAt +
				'}';
	}
}