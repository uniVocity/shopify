package com.univocity.shopify.model.db;



import com.univocity.shopify.model.db.core.*;
import com.univocity.shopify.model.shopify.*;

import java.sql.*;
import java.util.*;

import static com.univocity.shopify.utils.database.Converter.*;


public class LineItem extends ShopifyEntity<LineItem> {

	private Long orderId;
	private Long quantity;
	private String title;

	private Long productId;
	private Long variantId;

	private Product product;
	private Variant variant;

	public LineItem() {

	}

	public LineItem(ShopifyLineItem shopifyLineItem, Long shopId) {
		this.setShopifyId(shopifyLineItem.id);
		this.quantity = shopifyLineItem.quantity;
		this.title = shopifyLineItem.title;
		this.setShopId(shopId);
	}


	@Override
	protected void populateMap(Map<String, Object> map) {
		map.put("shopify_id", getShopifyId());
		map.put("title", getTitle());
		map.put("quantity", getQuantity());

		map.put("product_id", getProductId());
		map.put("variant_id", getVariantId());
		map.put("order_id", getOrderId());
	}

	@Override
	protected void populateFromResultSet(ResultSet rs, int rowNum) throws SQLException {
		setId(readLong(rs, "id"));
		setShopId(readLong(rs, "shop_id"));
		setShopifyId(readLong(rs, "shopify_id"));
		setOrderId(readLong(rs, "order_id"));
		setQuantity(readLong(rs, "quantity"));
		setTitle(rs.getString("title"));
		setVariantId(readLong(rs, "variant_id"));
		setProductId(readLong(rs, "product_id"));

	}

	public Long getQuantity() {
		return quantity == null ? 0 : quantity;
	}

	public void setQuantity(Long quantity) {
		this.quantity = quantity;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}


	public Long getProductId() {
		return productId;
	}

	public void setProductId(Long productId) {
		this.productId = productId;
		this.product = idUpdated(product, productId);
	}

	public Long getVariantId() {
		return variantId;
	}

	public void setVariantId(Long variantId) {
		this.variantId = variantId;
		this.variant = idUpdated(variant, variantId);
	}

	public Product getProduct() {
		return product;
	}

	public void setProduct(Product product) {
		this.product = product;
		this.productId = objectUpdated(product);
	}

	public Variant getVariant() {
		return variant;
	}

	public void setVariant(Variant variant) {
		this.variant = variant;
		this.variantId = objectUpdated(variant);
	}

	public Long getOrderId() {
		return orderId;
	}

	public void setOrderId(Long orderId) {
		this.orderId = orderId;
	}
}
