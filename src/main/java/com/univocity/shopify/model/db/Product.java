package com.univocity.shopify.model.db;


import com.univocity.shopify.model.shopify.*;

import java.sql.*;
import java.util.*;

import static com.univocity.shopify.dao.base.BaseDao.*;

public class Product extends ImageEntity<Product> {

	private String name;
	private Timestamp disabledAt;

	private List<Variant> variants = Collections.emptyList();

	public Product() {

	}

	@Override
	protected void toMap(Map<String, Object> map) {
		map.put("name", getName());
		map.put("disabled_at", getDisabledAt());
	}


	@Override
	protected void fromResultSet(ResultSet rs, int rowNum) throws SQLException {
		setName(rs.getString("name"));
		setDisabledAt(rs.getTimestamp("disabled_at"));
	}

	public Product(ShopifyProduct product, Long shopId) {
		this.setDisabledAt(now());
		this.setShopId(shopId);
		this.setShopifyId(product.id);
		this.setName(product.title);

		if (product.image != null) {
			this.setImage(new Image(product.image, shopId));
		}
		if (product.variants != null) {
			this.variants = new ArrayList<>();
			for (ProductVariant shopifyVariant : product.variants) {
				Image image = null;
				if (product.images != null) {
					for (ShopifyImage shopifyImage : product.images) {
						if (shopifyVariant.imageId != null && shopifyImage.id == shopifyVariant.imageId) {
							image = new Image(shopifyImage, shopId);
							break;
						}
					}
				}

				this.variants.add(new Variant(shopifyVariant, shopId, this, image));
			}
		}
	}

	public Product(ShopifyLineItem lineItem) {
		this.setShopifyId(lineItem.productId);
		this.name = lineItem.title;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Timestamp getDisabledAt() {
		return disabledAt;
	}

	public void setDisabledAt(Timestamp disabledAt) {
		this.disabledAt = disabledAt;
	}

	public boolean isEnabled() {
		return this.getId() != null && this.disabledAt == null;
	}

	public List<Variant> getVariants() {
		return variants;
	}
}
