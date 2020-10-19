package com.univocity.shopify.dao;


import com.univocity.shopify.dao.base.*;
import com.univocity.shopify.exception.*;
import com.univocity.shopify.model.db.*;
import com.univocity.shopify.model.shopify.*;
import org.slf4j.*;

import java.util.*;

import static com.univocity.shopify.dao.base.Operation.*;


/**
 * @author uniVocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 */
public class ProductDao extends ShopifyEntityDao<Product> {

	private static final Logger log = LoggerFactory.getLogger(ProductDao.class);

	private static final String QUERY_PRODUCTS_BY_ID = "SELECT * from product WHERE id = ? AND shop_id = ? AND deleted_at IS NULL";
	private static final String QUERY_PRODUCTS_BY_NAME = "SELECT * from product WHERE name = ? AND shop_id = ? AND deleted_at IS NULL";

	public ProductDao() {
		super("Product", true);
		enableCacheFor(QUERY_PRODUCTS_BY_ID, (Product product) -> new Object[]{product.getId(), product.getShopId()});
		enableCacheFor(QUERY_PRODUCTS_BY_NAME, (Product product) -> new Object[]{product.getName(), product.getShopId()});
	}

	@Override
	protected Product newEntity() {
		return new Product();
	}


	public Product getProduct(Long product_id, Long shopId) {
		return queryForOptionalEntity(QUERY_PRODUCTS_BY_ID, product_id, shopId);
	}

	public Product getProduct(String productName, Long shopId) {
		return queryForOptionalEntity(QUERY_PRODUCTS_BY_NAME, productName, shopId);
	}

	@Override
	protected void validate(Operation operation, Product product) {
		super.validate(operation, product);

		if (operation == INSERT) {
			if (getProduct(product.getName(), product.getShopId()) != null) {
				throw new ValidationException("Product '" + product.getName() + "' already registered.");
			}
		}
	}

	public Product persist(ShopifyLineItem shopifyLineItem, Long shopId) {
		Product product = getByShopifyId(shopId, shopifyLineItem.productId);
		if (product == null) {
			product = new Product(shopifyLineItem);
			product.setShopId(shopId);
			persist(product);
		}
		return product;
	}

	public List<Product> getProducts(Long shopId) {
		return queryForEntities("SELECT * FROM product WHERE shop_id = ? AND deleted_at IS NULL", shopId);
	}

	public void disableProductsOfShop(Long shopId) {
		java.sql.Timestamp now = now();
		int updateCount = db.update("UPDATE product SET disabled_at = ? WHERE shop_id = ?", new Object[]{now, shopId});
		log.warn("Disabled {} products of shop {}", updateCount, shopId);

		getProducts(shopId).forEach(product -> {
			product.setDisabledAt(now);
			evictCachedEntity(product).forEach(p -> p.setDisabledAt(now)); //ensures anything referencing the stale instance is updated (such as variants)
		});
	}
}