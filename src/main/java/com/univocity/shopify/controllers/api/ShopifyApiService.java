package com.univocity.shopify.controllers.api;


import com.univocity.shopify.*;
import com.univocity.shopify.dao.*;
import com.univocity.shopify.email.*;
import com.univocity.shopify.exception.*;
import com.univocity.shopify.model.shopify.*;
import com.univocity.shopify.utils.*;
import org.apache.commons.collections4.*;
import org.apache.commons.lang3.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.*;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.*;

import static com.univocity.shopify.utils.Utils.*;

/**
 * @author uniVocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 */
@RestController
@Import(ApplicationConfiguration.class)
public class ShopifyApiService {

	public static final String A_Z = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	public static final String a_z = "abcdefghijklmnopqrstuvwxyz";
	public static final String O_9 = "0123456789";

	private static final String PASSWORD_CHARS = A_Z + a_z + O_9;

	private static final Logger log = LoggerFactory.getLogger(ShopifyApiService.class);

	@Autowired
	private App utils;

	@Autowired
	ShopDao shops;

	@Autowired
	PropertyBasedConfiguration config;

	@Autowired
	SystemMailSender systemMailSender;

	@RequestMapping("/webhooks/register")
	public void registerWebHooks(@RequestParam("shop") String shopName/*, @RequestParam("topic") String topic, HttpServletRequest request*/) {
		registerWebHook(shopName, "orders/fulfilled", "/purchased");
		registerWebHook(shopName, "app/uninstalled", "/uninstalled");
		registerWebHook(shopName, "products/update", "/product/update");
	}

	private void registerWebHook(String shopName, String topic, String localEndpoint) {
		WebHookRegistration registration = new WebHookRegistration();
		registration.address = config.getProperty("public.address") + localEndpoint;
		registration.topic = topic;
		registration.format = "json";

		try {
			Webhook response = utils.postFor(Webhook.class, new Webhook(registration), shopName, "/admin/webhooks.json");
			if (response == null) {
				utils.notifyError("Error registering for webhook on topic '{}' for store '{}'. Registration: {}", topic, shopName, registration);
			}
			if (response.webhook != null && response.webhook.id != null) {
				log.info("Successfully registered webhook on topic '{}' of store '{}'", topic, shopName);
			} else {
				utils.notifyError("Failed to register webhook on topic '{}' of store '{}'", topic, shopName);
			}
		} catch (ShopifyErrorException e) {
			if (e.getBody() != null && e.getBody().toLowerCase().contains("for this topic has already been taken")) {
				log.info("Duplicate webhook registration on topic {} of store {}. Registration {}. Ignoring as it is registered.", topic, shopName, registration);
				return;
			} else {
				utils.notifyError(e, "Error registering for webhook on topic {} for store {}. Registration: {}", topic, shopName, registration);
			}
		} catch (Exception e) {
			utils.notifyError(e, "Error registering for webhook on topic {} for store {}. Registration: {}", topic, shopName, registration);
		}
	}


	@RequestMapping("/shopify/webhooks")
	public WebhookList getWebhooks(@RequestParam("shop") String shopName, HttpServletRequest request) {
		return executeLocal(request, () -> {
			WebhookList webhooks = utils.getFor(WebhookList.class, shopName, "/admin/webhooks.json");
			return webhooks;
		});
	}

	@RequestMapping("/shopify/webhooks/count")
	public int getWebhookCount(@RequestParam("shop") String shopName, HttpServletRequest request) {
		return executeLocal(request, () -> {
			Count count = utils.getFor(Count.class, shopName, "/admin/webhooks/count.json");
			return count.count;
		});
	}

	@RequestMapping("/shopify/webhook/{id}")
	public Webhook getWebhook(@PathVariable("id") String webhookId, @RequestParam("shop") String shopName, HttpServletRequest request) {
		return executeLocal(request, () -> utils.getFor(Webhook.class, shopName, "/admin/webhooks/" + webhookId + ".json"));
	}

	@RequestMapping("/shopify/webhook/{id}/delete")
	public void deleteWebhook(@PathVariable("id") String webhookId, @RequestParam("shop") String shopName, HttpServletRequest request) {
		executeLocal(request, () -> utils.delete(shopName, "/admin/webhooks/" + webhookId + ".json"));
	}

	@RequestMapping("/shopify/webhook/{id}/update")
	public Webhook updateWebhook(@PathVariable("id") String webhookId, @RequestParam("shop") String shopName, @RequestParam("new_address") String newServerAddress, HttpServletRequest request) {
		return executeLocal(request, () -> {
			Webhook update = new Webhook();
			update.webhook = new WebHookRegistration();
			update.webhook.id = Long.valueOf(webhookId);
			update.webhook.address = newServerAddress;
			return utils.putFor(Webhook.class, update, shopName, "/admin/webhooks/" + webhookId + ".json");
		});
	}

	@RequestMapping("/shopify/customer/{customer_id}/orders")
	public OrderList getOrders(@PathVariable("customer_id") String customerId, @RequestParam("shop") String shopName, HttpServletRequest request) {
		return executeLocal(request, () -> {
			OrderList out = utils.getFor(OrderList.class, shopName, "/admin/orders.json?customer_id=" + customerId + "&status=closed");
			return out;
		});
	}

	@RequestMapping("/shopify/customer/{customer_id}/order/{order_id}")
	public ShopifyOrder getOrder(@PathVariable("customer_id") String customerId, @PathVariable("order_id") String order_id, @RequestParam("shop") String shopName, HttpServletRequest request) {
		return executeLocal(request, () -> {
			final String endpoint = "/admin/orders.json?ids=" + order_id + "&customer_id=" + customerId + "&status=closed";
			OrderList out = utils.getFor(OrderList.class, shopName, endpoint);
			if (CollectionUtils.isNotEmpty(out.orders)) {
				if (out.orders.size() != 1) {
					log.warn("Got multiple ({}) orders from shopify service. Shop: {}, Endpoint: {}", out.orders.size(), shopName, endpoint);
				}
				return out.orders.get(0);
			}
			return null;
		});
	}

	@RequestMapping("/shopify/order_json/{order_id}")
	public String getOrderJson(@PathVariable("order_id") String order_id, @RequestParam("shop") String shopName, HttpServletRequest request) {
		return executeLocal(request, () -> {
			final String endpoint = "/admin/orders.json?ids=" + order_id + "&status=closed";
			String out = utils.getFor(String.class, shopName, endpoint);
			if (StringUtils.isNotBlank(out)) {
				return out;
			}
			return null;
		});
	}

	@RequestMapping("/shopify/product/{id}")
	public ShopifyProduct getProduct(@RequestParam("shop") String shopName, @PathVariable("id") String id, HttpServletRequest request) {
		return executeLocal(request, () -> {
			ProductWrapper out = utils.getFor(ProductWrapper.class, shopName, "/admin/products/" + id + ".json?fields=id,title,variants,image,images");
			return out.product;
		});
	}

	@RequestMapping("/shopify/variant/{id}")
	public ProductVariant getVariant(@RequestParam("shop") String shopName, @PathVariable("id") String id, HttpServletRequest request) {
		return executeLocal(request, () -> {
			ProductVariantWrapper out = utils.getFor(ProductVariantWrapper.class, shopName, "/admin/variants/" + id + ".json");
			return out.variant;
		});
	}

	@RequestMapping("/shopify/product/{product_id}/image/{id}")
	public ShopifyImage getImage(@RequestParam("shop") String shopName, @PathVariable("product_id") String productId, @PathVariable("id") String imageId, HttpServletRequest request) {
		return executeLocal(request, () -> {
			ImageWrapper out = utils.getFor(ImageWrapper.class, shopName, "/admin/products/" + productId + "/images/" + imageId + ".json");
			return out.image;
		});
	}

	@RequestMapping("/shopify/products")
	public ProductList getProducts(@RequestParam("shop") String shopName, HttpServletRequest request) {
		return executeLocal(request, () -> {
			ProductList out = utils.getFor(ProductList.class, shopName, "/admin/products.json?limit=250&fields=id,title,variants,image");
			return out;
		});
	}

	@RequestMapping("/shopify/shop/details")
	public ShopDetails getShopDetails(@RequestParam("shop") String shopName, HttpServletRequest request) {
		return executeLocal(request, () -> {
			ShopDetailsWrapper shopDetails = utils.getFor(ShopDetailsWrapper.class, shopName, "/admin/shop.json");
			if (shopDetails != null) {
				return shopDetails.shop;
			}

			return null;
		});
	}

	@RequestMapping("/shopify/products/count")
	public int getProductCount(@RequestParam("shop") String shopName, HttpServletRequest request) {
		return executeLocal(request, () -> {
			Count count = utils.getFor(Count.class, shopName, "/admin/products/count.json");
			return count.count;
		});
	}
}
