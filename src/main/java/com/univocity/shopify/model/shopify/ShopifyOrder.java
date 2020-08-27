package com.univocity.shopify.model.shopify;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.*;
import com.univocity.shopify.model.shopify.serializer.*;
import com.univocity.shopify.utils.*;

import java.math.*;
import java.time.*;
import java.util.*;
import java.util.stream.*;

public class ShopifyOrder {

	@JsonProperty
	public Long id;


	@JsonProperty
	public String number;

	@JsonProperty("order_number")
	public Long orderNumber;

	@JsonProperty
	public String note;

	@JsonProperty
	public String name;

	@JsonProperty
	public String email;

	@JsonProperty("order_status_url")
	public String orderStatusUrl;

	@JsonProperty("contact_email")
	public String contactEmail;

	@JsonProperty(value = "total_price")
	public BigDecimal totalPrice;

	@JsonProperty(value = "total_price_usd")
	public BigDecimal totalPriceUsd;

	@JsonProperty
	public BigDecimal discount;

	@JsonProperty(value = "financial_status")
	public String financialStatus;

	@JsonProperty
	public ShopifyCustomer customer;

	@JsonProperty("line_items")
	public List<ShopifyLineItem> lineItems;

	@JsonProperty("billing_address")
	public ShopifyAddress billingAddress;

	@JsonProperty("created_at")
	@JsonDeserialize(using = ShopifyDateDeserializer.class)
	@JsonSerialize(using = ShopifyDateSerializer.class)
	public ZonedDateTime createdAt;

	@JsonProperty("updated_at")
	@JsonDeserialize(using = ShopifyDateDeserializer.class)
	@JsonSerialize(using = ShopifyDateSerializer.class)
	public ZonedDateTime updatedAt;

	@JsonProperty("closed_at")
	@JsonDeserialize(using = ShopifyDateDeserializer.class)
	@JsonSerialize(using = ShopifyDateSerializer.class)
	public ZonedDateTime closedAt;

	@JsonProperty("cancelled_at")
	@JsonDeserialize(using = ShopifyDateDeserializer.class)
	@JsonSerialize(using = ShopifyDateSerializer.class)
	public ZonedDateTime cancelledAt;

	@JsonProperty("cancel_reason")
	public String cancelReason;

	@JsonProperty
	public String token;

	public ShopifyOrder() {

	}

	@Override
	public String toString() {
		return "ShopifyOrder{" +
				"id=" + id +
				", number='" + number + '\'' +
				", orderNumber='" + orderNumber + '\'' +
				", note='" + note + '\'' +
				", name='" + name + '\'' +
				", email='" + email + '\'' +
				", orderStatusUrl='" + orderStatusUrl + '\'' +
				", contactEmail='" + contactEmail + '\'' +
				", totalPrice=" + totalPrice +
				", discount=" + discount +
				", financialStatus='" + financialStatus + '\'' +
				", customer=" + customer +
				", lineItems=" + lineItems +
				", createdAt=" + createdAt +
				", updatedAt=" + updatedAt +
				", closedAt=" + closedAt +
				", cancelledAt=" + cancelledAt +
				", cancelReason='" + cancelReason + '\'' +
				", token='" + token + '\'' +
				'}';
	}

	public Set<Long> getProductIds() {
		if (lineItems != null) {
			return lineItems.stream()
					.filter(li -> li != null && li.name != null)
					.map(ShopifyLineItem::getProductId)
					.collect(Collectors.toSet());
		}
		return Collections.emptySet();
	}

	public java.sql.Date getPurchaseDate(){
		ZonedDateTime out = closedAt;
		if(out == null){
			out = updatedAt;
		}
		if(out == null){
			out = createdAt;
		}
		return Utils.toSqlDate(out);
	}
}
