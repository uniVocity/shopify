package com.univocity.shopify.dao.base;


import com.univocity.shopify.model.db.core.*;

/**
 * @author uniVocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 */
public abstract class ShopifyEntityDao<T extends ShopifyEntity<T>> extends ShopEntityDao<T> {

	private final String queryByShopifyId;

	public ShopifyEntityDao(String table) {
		this(table, false);
	}

	public ShopifyEntityDao(String table, boolean cacheByShopifyId) {
		this(table, table, cacheByShopifyId);
	}

	public ShopifyEntityDao(String table, String entityName) {
		this(table, entityName, false);
	}

	public ShopifyEntityDao(String table, String entityName, boolean cacheByShopifyId) {
		super(table, entityName);
		queryByShopifyId = "SELECT * FROM " + table.toLowerCase() + " WHERE shopify_id = ? AND shop_id = ? AND deleted_at IS NULL";
		if (cacheByShopifyId) {
			super.enableCacheFor(queryByShopifyId, e -> new Object[]{e.getShopifyId(), e.getShopId()});
		}
	}

	@Override
	protected abstract T newEntity();

	public final T getByShopifyId(String shopName, Long shopifyId) {
		return getByShopifyId(shops.getShopId(shopName), shopifyId);
	}

	public final T getByShopifyId(Long shopId, Long shopifyId) {
		validateIds(shopId, shopifyId);
		return queryForOptionalEntity(queryByShopifyId, shopifyId, shopId);
	}

	public final Long getIdByShopifyId(String shopName, Long shopifyId) {
		return getIdByShopifyId(shops.getShopId(shopName), shopifyId);
	}

	public final Long getIdByShopifyId(Long shopId, Long shopifyId) {
		validateIds(shopId, shopifyId);
		return db.queryForObject("SELECT id FROM " + table.toLowerCase() + " WHERE shopify_id = ? AND shop_id = ? AND deleted_at IS NULL", Number.class, shopifyId, shopId).longValue();
	}

}
