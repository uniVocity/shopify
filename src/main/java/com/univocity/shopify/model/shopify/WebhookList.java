package com.univocity.shopify.model.shopify;

import com.fasterxml.jackson.annotation.*;

import java.util.*;

public class WebhookList {
	@JsonProperty(value = "webhooks")
	private List<WebHookRegistration> webhooks;

	@Override
	public String toString() {
		return "WebhookList{" +
				"webhooks=" + webhooks +
				'}';
	}
}
