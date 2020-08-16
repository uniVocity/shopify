

package com.univocity.shopify.model.shopify;

import com.fasterxml.jackson.annotation.*;

public class ShopDetails {

	@JsonProperty("id")
	public long id;

	@JsonProperty("customer_email")
	public String customerEmail;

	@JsonProperty("email")
	public String email;

	@JsonProperty("domain")
	public String domain;

	@JsonProperty("phone")
	public String phone;

	@JsonProperty("shop_owner")
	public String shopOwner;

	@JsonProperty("timezone")
	public String timezone;

	@JsonProperty("country_code")
	public String country;

	@JsonProperty("city")
	public String city;

	@Override
	public String toString() {
		return "ShopDetails{" +
				"id=" + id +
				", customerEmail=" + customerEmail +
				", email=" + email +
				", domain=" + domain +
				", phone=" + phone +
				", shopOwner=" + shopOwner +
				", timezone=" + timezone +
				", country=" + country +
				", city=" + city +
				"}";
	}
}
