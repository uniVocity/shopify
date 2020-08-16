package com.univocity.shopify.model.shopify;

import com.fasterxml.jackson.annotation.*;

public class Webhook {
	@JsonProperty(value = "webhook")
	public WebHookRegistration webhook;

	public Webhook() {

	}

	public Webhook(WebHookRegistration webhook) {
		this.webhook = webhook;
	}

	@Override
	public String toString() {
		return "Webhook{" +
				"webhook=" + webhook +
				'}';
	}
}
