package com.univocity.shopify.dao.base;

import com.univocity.shopify.model.db.core.*;
import org.slf4j.*;

import static com.univocity.shopify.utils.Utils.*;

/**
 * @author uniVocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 */
public abstract class ShopEntityDao<T extends ShopEntity<T>> extends EntityDao<T> {

	private static final Logger log = LoggerFactory.getLogger(ShopEntityDao.class);

	public ShopEntityDao(String table) {
		super(table);
	}

	public ShopEntityDao(String table, String entityName) {
		super(table, entityName);
	}

	protected abstract T newEntity();

	@Override
	protected String[] identifierNames() {
		return new String[]{"id", "shop_id"};
	}

	@Override
	protected Object[] identifierValues(T entity) {
		return new Object[]{entity.getId(), entity.getShopId()};
	}

	@Override
	protected void validate(Operation operation, T entity) {
		if (entity.getShopId() == null) {
			throw new IllegalArgumentException(newMessage("Cannot " + operation.toString().toLowerCase() + " {} without a valid shop ID", entityName));
		}
	}

	protected final void validateIds(Long shopId, Long id) {
		notNull(shopId, "Shop ID for entity {} with ID {}", entityName, id);
		notNull(shopId, "ID for entity {} with Shop ID {}", entityName, shopId);
	}

	public final T getById(String shopName, Long id) {
		return getById(shops.getShopId(shopName), id);
	}

	public final T getById(Long shopId, Long id) {
		validateIds(shopId, id);
		return queryForEntity("SELECT * FROM " + table + " WHERE id = ? AND shop_id = ? AND deleted_at IS NULL", id, shopId);
	}

	public final T getOptionalEntityById(String shopName, Long id) {
		return getOptionalEntityById(shops.getShopId(shopName), id);
	}

	public final T getOptionalEntityById(Long shopId, Long id) {
		validateIds(shopId, id);
		return queryForOptionalEntity("SELECT * FROM " + table + " WHERE id = ? AND shop_id = ? AND deleted_at IS NULL", id, shopId);
	}

}
