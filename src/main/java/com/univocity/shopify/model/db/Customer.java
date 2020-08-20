package com.univocity.shopify.model.db;

import com.univocity.shopify.model.db.core.*;
import com.univocity.shopify.model.shopify.*;
import com.univocity.shopify.utils.*;

import java.sql.*;
import java.util.*;

import static com.univocity.shopify.utils.Utils.*;
import static com.univocity.shopify.utils.database.Converter.*;


public class Customer extends ShopifyEntity<Customer> {

	private String email;
	private String firstName;
	private String lastName;

	public Customer() {
	}

	public Customer(ShopifyCustomer shopifyCustomer, Long shopId) {
		this.email = shopifyCustomer.email;
		this.firstName = shopifyCustomer.firstName;
		this.lastName = shopifyCustomer.lastName;
		this.setCreatedAt(toTimestamp(shopifyCustomer.createdAt));
		this.setUpdatedAt(toTimestamp(shopifyCustomer.updatedAt));
		this.setShopId(shopId);
		this.setShopifyId(shopifyCustomer.id);
	}

	@Override
	protected void populateMap(Map<String, Object> map) {
		map.put("email", getEmail());
		map.put("first_name", getFirstName());
		map.put("last_name", getLastName());

		if (getCreatedAt() != null) {
			map.put("created_at", getCreatedAt());
		}

		if (getUpdatedAt() != null) {
			map.put("updated_at", getUpdatedAt());
		}
	}

	@Override
	protected void populateFromResultSet(ResultSet rs, int rowNum) throws SQLException {
		setId(readLong(rs, "id"));
		setShopId(readLong(rs, "shop_id"));
		setShopifyId(readLong(rs, "shopify_id"));
		setFirstName(rs.getString("first_name"));
		setLastName(rs.getString("last_name"));
		setEmail(rs.getString("email"));
		setCreatedAt(rs.getTimestamp("created_at"));
		setUpdatedAt(rs.getTimestamp("updated_at"));
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}


	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getName() {
		return Utils.getName(getFirstName(), getLastName(), "customer");
	}
}
