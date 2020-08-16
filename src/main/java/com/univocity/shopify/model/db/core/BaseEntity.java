package com.univocity.shopify.model.db.core;


import com.univocity.parsers.common.input.*;
import com.univocity.shopify.dao.base.*;
import com.univocity.shopify.utils.*;
import org.springframework.jdbc.core.*;

import java.sql.*;
import java.util.*;

import static com.univocity.shopify.utils.database.Converter.*;

/**
 * @author uniVocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 */
public abstract class BaseEntity<T extends BaseEntity> implements RowMapper<T>, Cloneable {

	private Long id;

	private Timestamp createdAt;
	private Timestamp updatedAt;
	private Timestamp deletedAt;

	private static final LinkedHashSet<String> identifierNames = new LinkedHashSet<>(Arrays.asList("id"));
	protected LinkedHashSet<String> getIdentifierNames() {
		return identifierNames;
	}

	private static final Set<String> columnsToIgnoreOnToString = new HashSet<>(Arrays.asList("created_at", "updated_at", "deleted_at"));
	protected Set<String> getColumnsToIgnoreOnToString(){
		return columnsToIgnoreOnToString;
	}

	public final Long getId() {
		return id;
	}

	public final void setId(Long id) {
		this.id = id;
	}

	public final Timestamp getCreatedAt() {
		return createdAt;
	}

	public final void setCreatedAt(Timestamp createdAt) {
		this.createdAt = createdAt;
	}

	public final Timestamp getUpdatedAt() {
		return updatedAt;
	}

	public final void setUpdatedAt(Timestamp updatedAt) {
		this.updatedAt = updatedAt;
	}

	public Timestamp getDeletedAt() {
		return deletedAt;
	}

	public void setDeletedAt(Timestamp deletedAt) {
		this.deletedAt = deletedAt;
	}

	public final Map<String, Object> toMap() {
		Map<String, Object> map = new HashMap<>();
		map.put("id", getId());
		fillMap(map);
		return map;
	}

	@Override
	public final T mapRow(ResultSet rs, int rowNum) throws SQLException {
		setId(readLong(rs, "id"));

		setCreatedAt(rs.getTimestamp("created_at"));
		setUpdatedAt(rs.getTimestamp("updated_at"));
		setDeletedAt(rs.getTimestamp("deleted_at"));

		readRow(rs, rowNum);

		return (T) this;
	}

	protected abstract void fillMap(Map<String, Object> map);

	protected abstract void readRow(ResultSet rs, int rowNum) throws SQLException;

	private final void append(ElasticCharAppender out, String key, Object value, boolean first) {
		if(!first){
			out.append("; ");
		}
		out.append(key);
		out.append('=');
		if (value instanceof String) {
			out.append('\'');
			out.append(String.valueOf(value));
			out.append('\'');
		} else {
			out.append(String.valueOf(value));
		}
	}

	@Override
	public final String toString() {
		Map<String, Object> map = new TreeMap<>(toMap());

		ElasticCharAppender out = Utils.borrowBuilder();
		try {
			out.append(getClass().getSimpleName());
			out.append('{');

			boolean first = true;
			for (String identifier : getIdentifierNames()) {
				append(out, identifier, map.get(identifier), first);
				first = false;
			}

			for (Map.Entry<String, Object> e : map.entrySet()) {
				if (getColumnsToIgnoreOnToString().contains(e.getKey()) || getIdentifierNames().contains(e.getKey())) {
					continue;
				}
				append(out, e.getKey(), e.getValue(), first);
				first = false;
			}

			out.append('}');
			return out.toString();
		} finally {
			Utils.releaseBuilder(out);
		}
	}

	protected static <T extends ShopEntity> T idUpdated(T currentObject, Long id) {
		if (currentObject != null) {
			if (currentObject.getId() == null) {
				currentObject.setId(id);
			} else if (!currentObject.getId().equals(id)) {
				currentObject = null;
			}
		}
		return currentObject;
	}

	protected static Long objectUpdated(ShopEntity object) {
		if (object == null) {
			return null;
		} else {
			return object.getId();
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || !(o instanceof BaseEntity)) {
			return false;
		}

		BaseEntity that = ((BaseEntity) o);

		if(this.id != null){
			if(that.id != null){
				return this.id.equals(that.id);
			}
		}
		return super.equals(o);
	}

	@Override
	public int hashCode() {
		if(this.id != null){
			return id.hashCode();
		}
		return super.hashCode();
	}

	@Override
	public T clone(){
		try {
			return (T) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new IllegalStateException(e);
		}
	}

	public void initializeDependencies(EntityDao dao){

	}
}
