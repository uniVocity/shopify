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

	@JsonProperty("accepts_marketing")
	public boolean acceptsMarketing;

	@JsonProperty("verified_email")
	public boolean verifiedEmail;

	@JsonProperty("tax_exempt")
	public boolean taxExempt;

	@JsonProperty
	public String phone;

	@JsonProperty
	public String note;


	@JsonProperty
	public String password;

	@JsonProperty("password_confirmation")
	public String passwordConfirmation;


	@JsonProperty("send_email_welcome")
	public boolean sendEmailWelcome;


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
				", acceptsMarketing=" + acceptsMarketing +
				", verifiedEmail=" + verifiedEmail +
				", taxExempt=" + taxExempt +
				", phone='" + phone + '\'' +
				", note='" + note + '\'' +
				", password='" + password + '\'' +
				", passwordConfirmation='" + passwordConfirmation + '\'' +
				", sendEmailWelcome=" + sendEmailWelcome +
				'}';
	}
}