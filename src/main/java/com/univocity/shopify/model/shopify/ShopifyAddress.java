package com.univocity.shopify.model.shopify;

import com.fasterxml.jackson.annotation.*;

public class ShopifyAddress {
	@JsonProperty
	public Long id;

	@JsonProperty(value = "first_name")
	public String firstName;

	@JsonProperty(value = "last_name")
	public String lastName;

	@JsonProperty
	public String company;

	@JsonProperty
	public String country;

	@JsonProperty
	public String city;

	@JsonProperty
	public String province;

	@JsonProperty
	public String zip;

	@JsonProperty
	public String phone;

	@JsonProperty
	public String address1;

	@JsonProperty
	public String address2;

	@JsonProperty
	public String name;

	@JsonProperty("province_code")
	public String provinceCode;

	@JsonProperty("country_code")
	public String countryCode;

	@JsonProperty("country_name")
	public String countryName;

	@JsonProperty("default")
	public boolean defaultAddress;

	@JsonProperty
	public String longitude;

	@JsonProperty
	public String latitude;

	public ShopifyAddress() {

	}

	@Override
	public String toString() {
		return "Address{" +
				"id=" + id +
				", firstName='" + firstName + '\'' +
				", lastName='" + lastName + '\'' +
				", company='" + company + '\'' +
				", country='" + country + '\'' +
				", city='" + city + '\'' +
				", province='" + province + '\'' +
				", zip='" + zip + '\'' +
				", phone='" + phone + '\'' +
				", address1='" + address1 + '\'' +
				", address2='" + address2 + '\'' +
				", name='" + name + '\'' +
				", provinceCode='" + provinceCode + '\'' +
				", countryCode='" + countryCode + '\'' +
				", countryName='" + countryName + '\'' +
				", defaultAddress=" + defaultAddress +
				", longitude='" + longitude + '\'' +
				", latitude='" + latitude + '\'' +
				'}';
	}
}
