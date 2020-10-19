package com.univocity.shopify.model.db;


import com.univocity.shopify.model.shopify.*;
import org.apache.commons.lang3.*;

import java.math.*;
import java.sql.*;
import java.util.*;

import static com.univocity.shopify.utils.database.Converter.*;

public class Variant extends ImageEntity<Variant> {

	private String sku;
	private String description;

	private Long productId;
	private Product product;

	private BigDecimal price;

	public Variant() {

	}

	public Variant(ShopifyLineItem lineItem) {
		setShopifyId(lineItem.variantId);
		setDescription(lineItem.variantTitle);
		setSku(lineItem.sku);
		setPrice(lineItem.price);
	}

	public Variant(ProductVariant variant, Long shopId, Product product, Image image) {
		setProduct(product);
		setShopifyId(variant.id);
		setDescription(variant.title);
		setSku(variant.sku);
		setImage(image);
		setShopId(shopId);
		setPrice(variant.price);
	}

	@Override
	protected void toMap(Map<String, Object> map) {
		map.put("description", getDescription());
		map.put("sku", getSku());
		map.put("product_id", getProductId());
		map.put("price", getPrice());

	}

	@Override
	protected void fromResultSet(ResultSet rs, int rowNum) throws SQLException {
		setDescription(rs.getString("description"));
		setProductId(readLong(rs, "product_id"));
		setSku(rs.getString("sku"));
		setPrice(rs.getBigDecimal("price"));

	}

	public Product getProduct() {
		return product;
	}

	public void setProduct(Product product) {
		this.product = product;
		this.productId = objectUpdated(product);
		if (product != null) {
			this.setShopId(product.getShopId());
		}
	}

	public Long getProductId() {
		return productId;
	}

	public void setProductId(Long productId) {
		this.productId = productId;
		this.product = idUpdated(product, productId);
	}

	public String getSku() {
		return sku;
	}

	public void setSku(String sku) {
		if (StringUtils.isNotBlank(sku)) {
			this.sku = sku;
		} else {
			this.sku = null;
		}
	}

	@Override
	public String getName() {
		if (product != null) {
			return product.getName() + " - " + getDescription();
		}
		return getDescription();
	}

	public String getDescription() {
		if (description == null) {
			return sku;
		}

		if ("default title".equalsIgnoreCase(description)) {
			return null;
		}

		return description;
	}

	public void setDescription(String description) {
		this.description = description == null ? null : description.trim();
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	@Override
	public boolean isEnabled() {
		if (getId() == null) {
			return false;
		}
		if (product == null) {
			throw new IllegalStateException("Product undefined for variant " + getId());
		}
		return product.isEnabled();
	}
}
