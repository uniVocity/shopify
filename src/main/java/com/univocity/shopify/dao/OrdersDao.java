package com.univocity.shopify.dao;

import com.univocity.shopify.dao.base.*;
import com.univocity.shopify.email.*;
import com.univocity.shopify.model.db.*;
import com.univocity.shopify.model.shopify.*;
import com.univocity.shopify.utils.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;

import java.util.*;

import static com.univocity.shopify.utils.Utils.*;

/**
 * @author uniVocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 */
public class OrdersDao extends ShopifyEntityDao<Order> {

	private static final Logger log = LoggerFactory.getLogger(OrdersDao.class);

	@Autowired
	CustomerDao customers;

	@Autowired
	LineItemDao lineItems;

	@Autowired
	ProductDao products;

	@Autowired
	SystemMailSender systemMailSender;


	public OrdersDao() {
		super("orders", "order");
	}

	@Override
	protected Order newEntity() {
		return new Order();
	}

	public void processOrder(String json, String shopName) {
		ShopifyOrder order = app.toObject(ShopifyOrder.class, json);
		if (order != null) {
			processOrder(order, shopName, json);
		}
	}

	public List<Order> getOrders(String shop) {
		return getOrders(shops.getShopId(shop));
	}

	public List<Order> getOrders(Long shop) {
		return queryForEntities("SELECT * FROM orders WHERE shop_id = ?", shop);
	}

	public List<Order> getCustomerOrders(String shop, Long customerId) {
		return queryForEntities("SELECT * FROM orders WHERE shop_id = ? AND customer_id = ?", shops.getShopId(shop), customerId);
	}

	public Order getCustomerOrderByToken(Long shopId, String shopifyOrderToken) {
		return queryForOptionalEntity("SELECT * FROM orders WHERE shop_id = ? AND token = ?", shopId, shopifyOrderToken);
	}

	public Order getCustomerOrder(Long shopId, String shopifyOrderId, String shopifyOrderToken) {
		return queryForOptionalEntity("SELECT * FROM orders WHERE shop_id = ? AND shopify_id = ? AND token = ?", shopId, shopifyOrderId, shopifyOrderToken);
	}

	public Order getOrder(Long shopId, String shopifyOrderId, String shopifyOrderToken) {
		return queryForOptionalEntity("" +
						"SELECT o.* FROM orders o " +
						"WHERE o.shop_id = ? AND o.shopify_id = ? AND o.token = ?",
				shopId, shopifyOrderId, shopifyOrderToken);
	}

	public void processOrder(ShopifyOrder shopifyOrder, String shopName, String json) {
		Set<Long> productsInOrder = shopifyOrder.getProductIds();
		if (productsInOrder.isEmpty()) {
			log.debug("Received shopify order with no products. Order ID '{}', shop '{}'", shopifyOrder.id, shopName);
		}

		final Long shopId = shops.getShopId(shopName);

		Order order = queryForOptionalEntity("SELECT * FROM orders WHERE shopify_id = ? AND shop_id = ? AND shopify_order_number = ? AND token = ?", shopifyOrder.id, shopId, shopifyOrder.orderNumber, shopifyOrder.token);
		if (order == null) {
			try {
				order = executeTransaction(transactionStatus -> {
					Order out = insert(shopifyOrder, shopId, json);
					if (out.getLineItems().isEmpty()) {
						throw new IllegalArgumentException(newMessage("No active items purchased in order ID {} of shop {}. Order number {}. Token {}", shopifyOrder.id, shopName, shopifyOrder.orderNumber, shopifyOrder.token));
					}
					return out;
				});
				log.info("Persisted order with products. Order ID {}, Shop ID {}. Shopify ID {}. Token {}", order.getId(), order.getShopId(), shopifyOrder.id, shopifyOrder.token);

			} catch (Exception ex) {
				String stacktrace = Utils.printStackTrace(ex);
				Number failedOrderId = null;
				try {
					try {
						failedOrderId = db.insertReturningKey("INSERT INTO failed_order (shop_id, order_json, stacktrace) VALUES (?,?,?)", "id",
								new Object[]{shops.getShopId(shopName), json, stacktrace});
					} catch (Exception e) {
						log.error("Error saving failed order details: " + json, e);
					}
					notifyError(ex, "Failed processing order with ID: " + failedOrderId);
				} finally {
					systemMailSender.sendErrorEmail("Error persisting order", "Unable to process an order. Manual intervention required.\nFailed order ID: " + failedOrderId + "\n\nORDER JSON:\n\n" + json + "\n", ex);
				}
			}
		} else {
			log.debug("Received duplicate shopify order ID '{}', shop '{}'", shopifyOrder.id, shopName);
		}
	}


	private Order insert(ShopifyOrder shopifyOrder, Long shopId, String json) {
		Customer customer;
		if (shopifyOrder.customer != null) {
			customer = customers.persist(shopifyOrder.customer, shopId);
		} else {
			log.debug("Received shopify order {} without customer information. Generating customer on the fly based on email and name for shop '{}'. Order details: {}", shopifyOrder.id, shops.getShopName(shopId), json);
			customer = new Customer();
			customer.setEmail(shopifyOrder.email);
			ShopifyAddress billingAddress = shopifyOrder.billingAddress;
			if (billingAddress != null && (billingAddress.firstName != null || billingAddress.lastName != null)) {
				customer.setFirstName(billingAddress.firstName);
				customer.setLastName(billingAddress.lastName);
			} else {
				customer.setFirstName(shopifyOrder.name);
			}

			customer = customers.persist(customer, shopId);
		}

		Order order = new Order(customer, shopifyOrder, shopId);
		order.setOriginalJson(json);

		persist(order);

		lineItems.persistLineItems(order, shopifyOrder, shopId);
		return order;
	}

}
