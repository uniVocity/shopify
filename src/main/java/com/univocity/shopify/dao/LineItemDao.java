package com.univocity.shopify.dao;

import com.univocity.shopify.dao.base.*;
import com.univocity.shopify.model.db.*;
import com.univocity.shopify.model.shopify.*;
import com.univocity.shopify.utils.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.jdbc.core.*;

import java.util.*;

import static com.univocity.shopify.utils.Utils.*;
import static com.univocity.shopify.utils.database.ExtendedJdbcTemplate.*;

/**
 * @author uniVocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 */
public class LineItemDao extends ShopifyEntityDao<LineItem> {
	private static final Logger log = LoggerFactory.getLogger(LineItemDao.class);

	private static String insertStatement;

	@Autowired
	OrdersDao orders;

	@Autowired
	ProductDao products;

	@Autowired
	VariantDao variants;

	@Override
	protected LineItem newEntity() {
		return new LineItem();
	}

	public LineItemDao() {
		super("line_item", "line item");
	}

	private RowMapper<LineItem> lineItemMapper = (rs, rowNum) -> {
		LineItem out = entityMapper.mapRow(rs, rowNum);

		if (out.getVariantId() != null) {
			Variant variant = variants.getVariant(out.getVariantId(), out.getProductId(), out.getShopId());
			out.setVariant(variant);
		}
		if (out.getProductId() != null) {
			Product product = products.getProduct(out.getProductId(), out.getShopId());
			if (product != null) {
				out.setProduct(product);
				if (out.getVariant() != null) {
					out.getVariant().setProduct(product);
				}
			}
		}
		return out;
	};

	public List<LineItem> persistLineItems(Order order, ShopifyOrder shopifyOrder, Long shopId) {
		List<Object[]> lineItems = new ArrayList<>();

		for (ShopifyLineItem shopifyLineItem : shopifyOrder.lineItems) {

			Product product = products.getByShopifyId(shopId, shopifyLineItem.productId);
			if (product == null) {
				// TODO: get from shopify and and insert
				log.debug("Skipping line item with product ID {} ({}). Unknown product", shopifyLineItem.productId, shopifyLineItem.name);
				continue;
			}
			if (product.isDisabled()) {
				log.debug("Skipping line item with product ID {} ({}). Product disabled", shopifyLineItem.productId, shopifyLineItem.name);
				continue;
			}

			Variant variant = variants.persist(product, shopifyLineItem, shopId);
			if (variant != null && variant.isDisabled()) {
				log.debug("Skipping line item with product ID {} ({}). Variant ID {}. Product disabled for variant", shopifyLineItem.productId, shopifyLineItem.name, shopifyLineItem.variantId);
				continue;
			}

			LineItem lineItem = new LineItem(shopifyLineItem, shopId);
			lineItem.setOrderId(order.getId());
			lineItem.setProduct(product);
			lineItem.setVariant(variant);

			Map<String, Object> values = lineItem.toMap();

			if (insertStatement == null) {
				insertStatement = generateInsertStatement("line_item", values.keySet().toArray(new String[0]));
			}

			lineItems.add(values.values().toArray(new Object[0]));
		}

		if (lineItems.isEmpty()) {
			return Collections.emptyList();
		}

		return executeTransaction(status -> {
			int[] rowsAffected = db.batchUpdate(insertStatement, lineItems);
			List<LineItem> lineItemList = loadLineItems(order.getId(), shopId);

			if (sum(rowsAffected) != lineItems.size() || lineItemList.size() != lineItems.size()) {
				String error = Utils.newMessage("Inconsistent state after persisting line items of shopify order {}. Line items to persist: {}, but persisted {} and fetched {}.", shopifyOrder.id, lineItems.size(), sum(rowsAffected), lineItemList.size());
				throw new IllegalStateException(error);
			}
			order.setLineItems(lineItemList);
			return lineItemList;
		});
	}

	public LineItem getLineItem(Long orderId, Long lineItemId, Long shopId) {
		return db.queryForOptionalObject("SELECT * FROM line_item WHERE order_id = ? AND id = ? AND shop_id = ?", lineItemMapper, orderId, lineItemId, shopId);
	}

	public List<LineItem> loadLineItems(Long orderId, String shopName) {
		return loadLineItems(orderId, shops.getShopId(shopName));
	}

	public List<LineItem> loadLineItems(Long orderId, Long shopId) {
		List<LineItem> lineItems = db.query(" SELECT * FROM line_item WHERE order_id = ? AND shop_id = ?", lineItemMapper, orderId, shopId);
		return lineItems;
	}
}
