package com.univocity.shopify.model.db.core;

import com.univocity.parsers.common.input.*;
import com.univocity.shopify.utils.*;

import java.sql.*;
import java.util.*;

import static com.univocity.shopify.utils.database.Converter.*;

/**
 * @author uniVocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 */
public abstract class ShopifyEntity<T extends ShopifyEntity<T>> extends ShopEntity<T> {

	private Long shopifyId;

	@Override
	protected final void addToMap(Map<String, Object> map) {
		map.put("shopify_id", shopifyId);
		populateMap(map);
	}

	@Override
	protected final void readFromRow(ResultSet rs, int rowNum) throws SQLException {
		setShopifyId(readLong(rs, "shopify_id"));
		populateFromResultSet(rs, rowNum);
	}

	public final Long getShopifyId() {
		return shopifyId;
	}

	public final void setShopifyId(Long shopifyId) {
		this.shopifyId = shopifyId;
	}

	protected abstract void populateMap(Map<String, Object> map);

	protected abstract void populateFromResultSet(ResultSet rs, int rowNum) throws SQLException;

	private static final LinkedHashSet<String> identifierNames = new LinkedHashSet<>(Arrays.asList("id", "shop_id", "shopify_id"));

	protected LinkedHashSet<String> getIdentifierNames() {
		return identifierNames;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || !(o instanceof ShopEntity)) {
			return false;
		}

		ShopifyEntity that = ((ShopifyEntity) o);

		if (this.shopifyId != null) {
			if (that.shopifyId != null) {
				if (!this.shopifyId.equals(that.shopifyId)) {
					return false;
				}
				return super.equals(o);
			}
			return false;
		}
		return super.equals(o);
	}

	@Override
	public int hashCode() {
		if (shopifyId != null && getId() != null && getShopId() != null) {
			int result = getId().hashCode();
			result = 31 * result + getShopId().hashCode();
			result = 31 * result + shopifyId.hashCode();
			return result;
		} else {
			return super.hashCode();
		}
	}

	public void appendDetails(ElasticCharAppender out) {
		Map<String, Object> tmp = new LinkedHashMap<>();
		populateMap(tmp);

		for (Map.Entry<String, Object> e : tmp.entrySet()) {
			if (Utils.isNotBlank(e.getValue())) {
				out.append("\n * ");
				out.append(e.getKey());
				out.append(": ");
				out.append(e.getValue());
			}
		}
	}
}
