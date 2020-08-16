package com.univocity.shopify.model.db.core;

import java.sql.*;
import java.util.*;

import static com.univocity.shopify.utils.database.Converter.*;

/**
 * @author uniVocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 */
public abstract class ShopEntity<T extends BaseEntity<T>> extends BaseEntity<T> {

	private Long shopId;

	public final Long getShopId() {
		return shopId;
	}

	public final void setShopId(Long shopId) {
		this.shopId = shopId;
	}

	@Override
	protected final void fillMap(Map<String, Object> map) {
		map.put("shop_id", getShopId());
		addToMap(map);
	}

	@Override
	protected final void readRow(ResultSet rs, int rowNum) throws SQLException {
		setShopId(readLong(rs, "shop_id"));
		readFromRow(rs, rowNum);
	}

	protected abstract void addToMap(Map<String, Object> map);

	protected abstract void readFromRow(ResultSet rs, int rowNum) throws SQLException;

	private static final LinkedHashSet<String> identifierNames = new LinkedHashSet<>(Arrays.asList("id", "shop_id"));
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

		ShopEntity that = ((ShopEntity) o);

		if(this.shopId != null){
			if(that.shopId != null){
				if(!this.shopId.equals(that.shopId)){
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
		if(shopId != null && getId() != null){
			int result = getId().hashCode();
			result = 31 * result + shopId.hashCode();
			return result;
		} else {
			return super.hashCode();
		}
	}
}
