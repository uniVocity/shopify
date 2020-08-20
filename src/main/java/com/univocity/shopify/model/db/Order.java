package com.univocity.shopify.model.db;



import com.univocity.shopify.model.db.core.*;
import com.univocity.shopify.model.shopify.*;
import com.univocity.shopify.utils.*;

import java.sql.*;
import java.util.*;

import static com.univocity.shopify.utils.Utils.*;
import static com.univocity.shopify.utils.database.Converter.*;

public class Order extends ShopifyEntity<Order> {

	private Long customerId;
	private Long shopifyOrderNumber;
	private String email;
	private String contactEmail;
	private Timestamp closedAt;
	private Timestamp cancelledAt;
	private String statusUrl;
	private String token;
	private String originalJson;

	private final List<LineItem> lineItems = new ArrayList<>();
	private Customer customer;

	public Order() {

	}

	@Override
	protected void populateMap(Map<String, Object> map) {
		map.put("shopify_order_number", getShopifyOrderNumber());
		map.put("email", getEmail());
		map.put("contact_email", getContactEmail());
		map.put("created_at", getCreatedAt());
		map.put("updated_at", getUpdatedAt());
		map.put("closed_at", getClosedAt());
		map.put("cancelled_at", getCancelledAt());
		map.put("status_url", getStatusUrl(null));
		map.put("token", getToken());
		map.put("original_json", getOriginalJson());
		map.put("customer_id", getCustomerId());
	}

	@Override
	protected void populateFromResultSet(ResultSet rs, int rowNum) throws SQLException {
		setShopifyOrderNumber(readLong(rs, "shopify_order_number"));
		setEmail(rs.getString("email"));
		setContactEmail(rs.getString("contact_email"));
		setStatusUrl(rs.getString("status_url"));
		setToken(rs.getString("token"));
		setClosedAt(rs.getTimestamp("closed_at"));
		setCancelledAt(rs.getTimestamp("cancelled_at"));
		setCustomerId(readLong(rs, "customer_id"));
	}

	public Order(Customer customer, ShopifyOrder order, Long shopId) {
		Utils.notNull(customer, "Customer");
		Utils.notNull(order, "Order");
		Utils.notNull(shopId, "Shop ID");

		this.setCustomer(customer);
		this.shopifyOrderNumber = order.orderNumber;
		this.email = order.email;
		this.contactEmail = order.contactEmail;
		this.setCreatedAt(toTimestamp(order.createdAt));
		this.setUpdatedAt(toTimestamp(order.updatedAt));
		this.closedAt = toTimestamp(order.closedAt);
		this.cancelledAt = toTimestamp(order.cancelledAt);
		this.statusUrl = order.orderStatusUrl;
		this.token = order.token;
		this.setShopId(shopId);
		this.setShopifyId(order.id);
	}

	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
		this.customerId = objectUpdated(customer);
	}

	public Long getShopifyOrderNumber() {
		return shopifyOrderNumber;
	}

	public void setShopifyOrderNumber(Long shopifyOrderNumber) {
		this.shopifyOrderNumber = shopifyOrderNumber;
	}


	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}


	public String getContactEmail() {
		return contactEmail;
	}

	public void setContactEmail(String contactEmail) {
		this.contactEmail = contactEmail;
	}

	public Timestamp getClosedAt() {
		return closedAt;
	}

	public void setClosedAt(Timestamp closedAt) {
		this.closedAt = closedAt;
	}


	public Timestamp getCancelledAt() {
		return cancelledAt;
	}

	public void setCancelledAt(Timestamp cancelledAt) {
		this.cancelledAt = cancelledAt;
	}


	public String getStatusUrl(String domain) {
		return getOrderStatusUrl(statusUrl, domain);
	}

	public void setStatusUrl(String statusUrl) {
		this.statusUrl = statusUrl;
	}


	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}


	public String getOriginalJson() {
		return originalJson;
	}

	public void setOriginalJson(String originalJson) {
		this.originalJson = originalJson;
	}

	public List<LineItem> getLineItems() {
		return lineItems;
	}

	public void setLineItems(List<LineItem> lineItems) {
		Utils.setTo(this.lineItems, lineItems);
	}

	public Long getCustomerId() {
		return customerId;
	}

	public void setCustomerId(Long customerId) {
		this.customerId = customerId;
		this.customer = idUpdated(customer, customerId);
	}

}
