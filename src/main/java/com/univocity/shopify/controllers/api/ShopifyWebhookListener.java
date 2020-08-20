package com.univocity.shopify.controllers.api;

import com.univocity.parsers.common.input.*;
import com.univocity.shopify.*;
import com.univocity.shopify.dao.*;
import com.univocity.shopify.model.db.*;
import com.univocity.shopify.model.shopify.*;
import com.univocity.shopify.service.*;
import com.univocity.shopify.utils.*;
import org.apache.commons.lang3.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.*;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.*;
import java.util.*;

import static com.univocity.shopify.utils.Utils.*;


/**
 * Shopify application endpoints.
 *
 * Settings available here: https://app.shopify.com/services/partners/api_clients/1611911/edit
 *
 * @author uniVocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 */

@RestController
@Import(ApplicationConfiguration.class)
public class ShopifyWebhookListener {
	private static final Logger log = LoggerFactory.getLogger(ShopifyWebhookListener.class);

	private static final Set<Long> processedMessageHashes = new CircularSet<>(16_000);

	static final String REDACTED_EMAIL = "redacted@email.com";
	static final String REDACTED_PHONE = "000000000";
	static final String REDACTED_FIELD = "redacted";

	@Autowired
	CustomerDao customerDao;

	@Autowired
	ShopifyApiService shopifyApiService;

	@Autowired
	ShopDao shops;

	@Autowired
	AuthenticationService authenticationService;

	@Autowired
	CredentialsDao credentials;

	@Autowired
	OrdersDao ordersDao;

	@Autowired
	ProductService productService;

	@Autowired
	App app;

	@Autowired
	SystemMessageDao systemMessageDao;

	private boolean isRequestValid(String description, String json, HttpServletRequest request) {

		Long hash = Utils.longHash(json);

		if (!"appUninstalled".equals(description)) {
			synchronized (processedMessageHashes) {
				if (processedMessageHashes.contains(hash)) {
					log.debug("Received duplicate webhook message {}. Discarding", description);
					return false;
				}
				processedMessageHashes.add(hash);
			}
		}

		if (authenticationService.isRequestValid(json, request)) {
			log.info("Received '{}' notification: {}", description, json);
			return true;
		} else {
			log.warn("Received INVALID '{}' notification: {}.\nInvalid Request: {}", json, Utils.printRequest(request));
			return false;
		}
	}

	void redactCustomer(String json, String shopName) {
		CustomerRedact redact = app.parseJson(CustomerRedact.class, json);

		log.info("Processing customer redact action for shop {}. Customer's Shopify ID {}. Shop's Shopify ID {}. Orders to redact: {}", redact.shop_domain, redact.customer.id, redact.shop_id, Arrays.toString(redact.orders_to_redact));

		Shop shop = shops.getShop(shopName);
		if (isMessageForThisShop(shop, shopName, json)) {

			Customer customer = customerDao.getByShopifyId(shop.getId(), redact.customer.id);
			if (customer != null) {
				log.info("Redacting data of customer ID {}. Shop {}", customer.getId(), shopName);
				customer.setEmail(REDACTED_EMAIL);
				customer.setFirstName(REDACTED_FIELD);
				customer.setLastName(REDACTED_FIELD);

				customerDao.persist(customer);

				List<Order> orders = ordersDao.getCustomerOrders(shopName, customer.getId());
				Set<Long> redactedOrders = new HashSet<>(Arrays.asList(redact.orders_to_redact));

				orders.removeIf(o -> !redactedOrders.contains(o.getShopifyId()));

				log.info("Redacting {} orders of customer ID {}. Shop {}", orders.size(), customer.getId(), shopName);

				for (Order order : orders) {
					order.setEmail(REDACTED_EMAIL);
					order.setContactEmail(REDACTED_EMAIL);
					order.setOriginalJson("{}");
					ordersDao.persist(order);
				}
			}
		}
	}

	String buildCustomerDataMessage(Shop shop, DataRequest redact) {
		Customer customer = customerDao.getByShopifyId(shop.getId(), redact.customer.id);
		if (customer != null) {
			log.info("Collecting data of customer ID {}. Shop {}", customer.getId(), shop.getShopName());

			ElasticCharAppender out = borrowBuilder();
			try {
				out.append("\r\n * Customer ID: ");
				out.append(customer.getShopifyId());
				customer.appendDetails(out);
				List<Order> orders = ordersDao.getCustomerOrders(shop.getShopName(), customer.getId());
				Set<Long> requestedOrders = new HashSet<>(Arrays.asList(redact.orders_requested));

				orders.removeIf(o -> !requestedOrders.contains(o.getShopifyId()));

				log.info("Collecting data of {} orders of customer ID {}. Shop {}", orders.size(), customer.getId(), shop.getShopName());


				out.append("\r\n\r\nDetails of ");
				out.append(" orders:");
				for (Order order : orders) {
					out.append("\r\n * Order ID ");
					out.append(order.getShopifyId());
					out.append("\r\n * E-mail ");
					out.append(order.getEmail());
					out.append("\r\n * Contact e-mail ");
					out.append(order.getContactEmail());
				}
				return out.toString();
			} finally {
				releaseBuilder(out);
			}
		}
		return null;
	}

	void processCustomerDataRequest(String json, String shopName) {
		DataRequest redact = app.parseJson(DataRequest.class, json);

		log.info("Processing customer data request for shop {}. Customer's Shopify ID {}. Shop's Shopify ID {}. Orders requested: {}", redact.shop_domain, redact.customer.id, redact.shop_id, Arrays.toString(redact.orders_requested));

		Shop shop = shops.getShop(shopName);
		if (isMessageForThisShop(shop, shopName, json)) {
			String out = buildCustomerDataMessage(shop, redact);
			if (out != null) {
				app.sendEmailToShopOwner(shopName, MessageType.DATA_REQUEST, s -> s.setText(out));
			}
		}
	}


	@RequestMapping(value = "/customer/data_request", method = RequestMethod.POST)
	public void customerDataRequest(@RequestBody String json, HttpServletRequest request) {
		if (isRequestValid("customer/data_request", json, request)) {
			processCustomerDataRequest(json, Utils.getShopName(request));
		}
	}

	@RequestMapping(value = "/customer/redact", method = RequestMethod.POST)
	public void customerRedact(@RequestBody String json, HttpServletRequest request) {
		if (isRequestValid("customer/redact", json, request)) {
			redactCustomer(json, Utils.getShopName(request));
		}
	}

	boolean isMessageForThisShop(Shop shop, String shopName, String json) {
		if (shop == null) {
			log.warn("Got redact request {} but no shop named '{}' was not found", json, shopName);
			return false;
		}
		if (!shop.getShopName().equalsIgnoreCase(shopName)) {
			app.notifyError("Got redact request {} but shop name in request '{}' doesn't match", json, shopName);
			return false;
		}
		return true;
	}

	void redactShop(String json, String shopName) {
		ShopRedact redact = app.parseJson(ShopRedact.class, json);

		log.info("Processing redact action on shop {}. Shop's Shopify ID {}.", redact.shop_domain, redact.shop_id);

		Shop shop = shops.getShopByShopifyIdIncludingDeleted(redact.shop_id);
		if (isMessageForThisShop(shop, shopName, json)) {
			shop.setOwnerEmail(REDACTED_EMAIL);
			shop.setCustomerEmail(REDACTED_EMAIL);
			shop.setPhone(REDACTED_PHONE);
			shop.setShopOwner(REDACTED_FIELD);
			shop.setCity(REDACTED_FIELD);
			shop.setNotificationEmailList(REDACTED_EMAIL);
			shop.setReplyToAddress(REDACTED_EMAIL);
			shop.setUseOwnMailServer(false);
			shop.setSmtpHost(null);
			shop.setSmtpUsername(null);
			shop.setSmtpPassword(null);
			shop.setSmtpSender(null);

			shops.persist(shop);
		}
	}

	@RequestMapping(value = "/shop/redact", method = RequestMethod.POST)
	public void shopRedact(@RequestBody String json, HttpServletRequest request) {
		if (isRequestValid("shop/redact", json, request)) {
			redactShop(json, Utils.getShopName(request));
		}
	}

	@RequestMapping(value = "/purchased", method = RequestMethod.POST)
	public void orderFulfilled(@RequestBody String json, HttpServletRequest request) {
		if (isRequestValid("orderFulfilled", json, request)) {
			String shopName = Utils.getShopName(request);
			ordersDao.processOrder(json, shopName);
		}
	}

	@RequestMapping(value = "/uninstalled", method = RequestMethod.POST)
	public void appUninstalled(@RequestBody String json, HttpServletRequest request) {
		if (isRequestValid("appUninstalled", json, request)) {
			String shopName = StringUtils.substringBetween(json, ",\"domain\":\"", "\"");

			if(StringUtils.isNotBlank(shopName)){
				if(!shopName.endsWith(".myshopify.com")){
					Shop shop = shops.getShopByDomain(shopName);
					if(shop != null){
						shopName = shop.getShopName();
					}
				}
			}

			log.debug("Checking if shop {} was not reinstalled.", shopName);
			int webhookCount = 0;
			try {
				webhookCount = shopifyApiService.getWebhookCount(shopName, null);
			} catch (Exception e) { //we want an exception here. It means the shop was not added back.
				log.debug("Got error {} trying to count number of webhooks of store {}. Processing app uninstall message.", e.getMessage(), shopName);
			}
			if (webhookCount == 0) {
				try {
					try {
						shops.deactivateShop(shops.getShop(shopName));
					} catch (Exception e) {
						app.notifyError("Error deactivating shop " + shopName, e);
					}
					log.info("Unregistering app from store {}", shopName);
					credentials.unregisterShop(shopName, json);

					Long shopId = shops.getShopIdIncludingDeleted(shopName);
					if (shopId != null) {
						app.sendEmailToShopOwner(shopName, MessageType.APP_UNINSTALLED);
					}
				} finally {
					shops.evict(shopName);
				}
			} else {
				log.info("Ingoring app uninstalled webhook as there are webhooks active for store {}", shopName);
			}
		}
	}

	@RequestMapping(value = "/product/update", method = RequestMethod.POST)
	public void productUpdated(@RequestBody String json, HttpServletRequest request) {
		if (isRequestValid("productUpdate", json, request)) {
			String shopName = Utils.getShopName(request);
			productService.updateFromShopifyWebhook(shopName, json);
		}
	}
}