package com.univocity.shopify.model.shopify;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.*;
import com.univocity.shopify.model.shopify.serializer.*;

import java.time.*;

/**
 * @author uniVocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 */
public class WebHookRegistration {

	@JsonProperty(value = "id")
	public Long id;

	@JsonProperty("topic")
	public String topic;

	@JsonProperty("address")
	public String address;

	@JsonProperty("format")
	public String format;

	@JsonProperty(value = "created_at")
	@JsonDeserialize(using = ShopifyDateDeserializer.class)
	@JsonSerialize(using = ShopifyDateSerializer.class)
	public ZonedDateTime createdAt;

	@JsonProperty(value = "updated_at")
	@JsonDeserialize(using = ShopifyDateDeserializer.class)
	@JsonSerialize(using = ShopifyDateSerializer.class)
	public ZonedDateTime updatedAt;

	public WebHookRegistration() {

	}

	@Override
	public String toString() {
		return "WebHookRegistration{" +
				"id=" + id +
				", topic='" + topic + '\'' +
				", address='" + address + '\'' +
				", format='" + format + '\'' +
				", createdAt=" + createdAt +
				", updatedAt=" + updatedAt +
				'}';
	}
}
