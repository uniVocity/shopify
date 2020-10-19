package com.univocity.shopify.dao;

import com.univocity.shopify.dao.base.*;
import com.univocity.shopify.model.db.*;
import com.univocity.shopify.model.shopify.*;
import org.slf4j.*;

import java.util.*;

/**
 * @author uniVocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 */
public class CustomerDao extends ShopifyEntityDao<Customer> {
	private static final Logger log = LoggerFactory.getLogger(CustomerDao.class);

	public CustomerDao() {
		super("customer");
	}

	@Override
	protected Customer newEntity() {
		return new Customer();
	}

	public Customer persist(Customer customer, String shopName) {
		return persist(customer, shops.getShopId(shopName));
	}

	public Customer persist(Customer customer, Long shopId) {
		Customer existingCustomer;
		if (customer.getShopifyId() == null) {
			existingCustomer = queryForOptionalEntity("SELECT * FROM customer WHERE email = ? AND shop_id = ? AND shopify_id IS NULL AND deleted_at IS NULL", new Object[]{customer.getEmail(), shopId});
		} else {
			existingCustomer = getByShopifyId(shopId, customer.getShopifyId());
		}

		if (existingCustomer == null) {
			Customer newCustomer = customer;
			newCustomer.setShopId(shopId);
			executeTransaction(transactionStatus -> insert(newCustomer));
			return newCustomer;
		} else {
			return existingCustomer;
		}
	}

	public Customer persist(ShopifyCustomer shopifyCustomer, String shopName) {
		return persist(shopifyCustomer, shops.getShopId(shopName));
	}

	public Customer persist(ShopifyCustomer shopifyCustomer, Long shopId) {
		Customer customer = getByShopifyId(shopId, shopifyCustomer.id);
		if (customer == null) {
			Customer newCustomer = new Customer(shopifyCustomer, shopId);
			executeTransaction(transactionStatus -> insert(newCustomer));
			customer = newCustomer;
		}
		return customer;
	}

	public List<Customer> getCustomers(String email, String shopName) {
		return getCustomers(email, shops.getShopId(shopName));
	}

	public List<Customer> getCustomers(String email, Long shopId) {
		return queryForEntities("SELECT * FROM customer WHERE shop_id = ? AND email = ? AND deleted_at IS NULL", shopId, email);
	}

	public List<Customer> getCustomers(String email, Long shopifyId, String shopName) {
		return getCustomers(email, shopifyId, shops.getShopId(shopName));
	}

	public List<Customer> getCustomers(String email, Long shopifyId, Long shopId) {
		return queryForEntities("SELECT * FROM customer WHERE shop_id = ? AND email = ? AND shopify_id AND deleted_at IS NULL", shopId, email, shopifyId);
	}

	public List<Customer> getCustomers(String shopName) {
		return getCustomers(shops.getShopId(shopName));
	}

	public List<Customer> getCustomers(Long shopId) {
		return queryForEntities("SELECT * FROM customer WHERE shop_id = ? AND deleted_at IS NULL", shopId);
	}

	public Customer getCustomer(String shopifyId, Long shopId) {
		return getByShopifyId(shopId, Long.parseLong(shopifyId));
	}

	public Customer getCustomerOfOrder(Long orderId, Long shopId){
		return queryForEntity("SELECT c.* FROM customer c JOIN order o ON o.id = ? AND o.customer_id = c.id AND o.shop_id = c.shop_id AND o.deleted_at IS NULL WHERE c.shop_id = ?", orderId, shopId);
	}
}
