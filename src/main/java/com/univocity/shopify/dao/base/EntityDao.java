package com.univocity.shopify.dao.base;

import com.univocity.shopify.exception.*;
import com.univocity.shopify.model.db.core.*;
import com.univocity.shopify.utils.*;
import org.slf4j.*;
import org.springframework.dao.*;
import org.springframework.jdbc.core.*;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import static com.univocity.shopify.utils.Utils.*;
import static com.univocity.shopify.utils.database.ExtendedJdbcTemplate.*;

/**
 * @author uniVocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 */
public abstract class EntityDao<T extends BaseEntity<T>> extends BaseDao {

	private static final Logger log = LoggerFactory.getLogger(EntityDao.class);

	protected final String table;
	protected final String entityName;

	private final String[] identifierNames;
	protected final LinkedHashSet<String> identifiers;

	private final SimpleCache<T> cache = new SimpleCache<>();
	private final Map<String, Function<Object[], T>> cachedQueries = new ConcurrentHashMap<>();
	private final Map<String, Function<T, Object[]>> cacheEviction = new ConcurrentHashMap<>();
	private final Map<Function<T, Object[]>, String> evictionParent = new ConcurrentHashMap<>();

	protected final RowMapper<T> entityMapper = (rs, rowNum) -> {
		T out = newEntity();
		out.mapRow(rs, rowNum);

		out.initializeDependencies(this);
		return out;
	};

	public EntityDao(String table) {
		this(table, table);
	}

	public EntityDao(String table, String entityName) {
		this.table = table.toLowerCase();
		this.entityName = entityName;

		this.identifierNames = identifierNames();
		this.identifiers = asLinkedHashSet(identifierNames);
	}

	protected abstract T newEntity();

	protected abstract String[] identifierNames();

	protected abstract Object[] identifierValues(T entity);

	protected abstract void validate(Operation operation, T entity);

	protected final String describe(T entity) {
		return printKeyValuePairs(identifierNames, identifierValues(entity));
	}

	public final T insert(T entity) {
		notNull(entity, "{} to insert", entityName);
		if (entity.getId() != null) {
			log.warn("Attempting to insert already persisted entity in {} with {}", table, describe(entity));
			return entity;
		}
		if (entity.getDeletedAt() != null) {
			log.info("Reinserting previously deleted entry deleted at {}. Identifier: ", entity.getDeletedAt(), describe(entity));
			entity.setDeletedAt(null);
		}

		validate(Operation.INSERT, entity);

		Map<String, Object> values = Collections.emptyMap();
		try {
			values = entity.toMap();
			Number id = db.insertReturningKey(table, "id", values);
			if (id == null) {
				notifyError("Error persisting {}. Could not obtain generated ID. Values: ", entityName, values);
			} else {
				entity.setId(id.longValue());
			}
		} catch (Exception e) {
			notifyError(e, "Error inserting into {}", table, values);
		} finally {
			evictCachedEntity(entity);
		}

		return entity;
	}

	protected List<T> evictCachedEntity(T entity) {
		List<T> entriesRemoved = new ArrayList<>();
		cacheEviction.values().forEach(keyGenerator -> {
			Object[] key = keyGenerator.apply(entity);
			T removed = cache.remove(evictionParent.get(keyGenerator), key);
			if (removed != null) {
				entriesRemoved.add(removed);
			}
		});
		return entriesRemoved;
	}

	public final void persist(T entity) {
		notNull(entity, "{} to persist", entityName);
		try {
			if (entity.getId() == null) {
				insert(entity);
			} else {
				if (entity.getDeletedAt() != null) {
					throw new IllegalStateException(newMessage("Can't update deleted entity {} ({})", entityName, describe(entity)));
				}
				validateIds(entity);
				validate(Operation.UPDATE, entity);

				Map<String, Object> values = entity.toMap();
				int affectedRowCount = db.update(table, values, identifiers, "deleted_at IS NULL");
				if (affectedRowCount <= 0) {
					log.warn("No rows affected on update to {}. Values: {}", table, values);
				} else if (affectedRowCount > 1) {
					notifyError("Multiple rows affected on update to {}. Values: ", table, values);
				}
			}
		} catch (ValidationException e) {
			throw e;
		} catch (Exception e) {
			notifyError(e, "Could not persist {}:", entityName, entity);
		} finally {
			evictCachedEntity(entity);
		}
	}

	public final void deleteHard(T entity) {
		notNull(entity, "{} to delete", entityName);
		validateIds(entity);
		try {
			String deleteSql = createDeleteStatement(table, identifierNames);
			validate(Operation.DELETE_HARD, entity);

			int affectedRowCount = db.update(deleteSql, identifierValues(entity));
			if (affectedRowCount <= 0) {
				log.warn("No rows affected deleting {}. {}", table, describe(entity));
			} else if (affectedRowCount > 1) {
				notifyError("Multiple rows affected deleting {}. {}", table, describe(entity));
			}
			entity.setDeletedAt(now());
		} catch (Exception e) {
			notifyError(e, "Could not hard delete {}", entityName);
		} finally {
			evictCachedEntity(entity);
		}
	}

	private final Object[] argsForDeletion(Timestamp time, T entity) {
		Object[] ids = identifierValues(entity);
		Object[] out = new Object[ids.length + 1];

		out[0] = time;
		for (int i = 1; i <= ids.length; i++) {
			out[i] = ids[i - 1];
		}

		return out;
	}

	public void deleteSoft(T entity) {
		notNull(entity, "{} to delete", entityName);
		validateIds(entity);
		if (entity.getDeletedAt() != null) {
			log.warn("Attempting to soft-delete already deleted entity {}. {}", table, describe(entity));
			return;
		}
		try {
			validate(Operation.DELETE_SOFT, entity);
			String deleteSql = createUpdateStatement(table, new String[]{"deleted_at"}, identifierNames, "deleted_at IS NULL");
			Timestamp now = now();
			int affectedRowCount = db.update(deleteSql, argsForDeletion(now, entity));
			if (affectedRowCount <= 0) {
				log.warn("No rows affected deleting {}. {}", table, describe(entity));
			} else if (affectedRowCount > 1) {
				notifyError("Multiple rows affected deleting {}. {}", table, describe(entity));
			}
			entity.setDeletedAt(now);
		} catch (Exception e) {
			notifyError(e, "Could not soft delete {}", entityName);
		} finally {
			evictCachedEntity(entity);
		}
	}

	public final void undelete(T entity) {
		notNull(entity, "{} to un-delete", entityName);
		validateIds(entity);
		try {
			validate(Operation.UNDELETE, entity);
			String undeleteSql = createUpdateStatement(table, new String[]{"deleted_at"}, identifierNames, "deleted_at IS NOT NULL");
			int affectedRowCount = db.update(undeleteSql, argsForDeletion(null, entity));
			if (affectedRowCount <= 0) {
				log.warn("No rows affected trying to un-delete {}. {}", table, describe(entity));
			} else if (affectedRowCount > 1) {
				notifyError("Multiple rows affected un-deleting {}. {}", table, describe(entity));
			}
			entity.setDeletedAt(null);
		} catch (Exception e) {
			notifyError(e, "Could not un-delete {}", entityName);
		} finally {
			evictCachedEntity(entity);
		}
	}

	protected final void validateIds(T entity) {
		Object[] ids = identifierValues(entity);
		sameSize(ids, identifierNames, "Identifier values", "Identifiers");

		for (int i = 0; i < ids.length; i++) {
			if (ids[i] == null) {
				notNull(ids[i], "Identifier {} of entity {}. [{}]", identifierNames[i], entityName, describe(entity));
			}
		}
	}

	public final T queryForFirstEntity(String query, Object... params) {
		try {
			return db.queryForFirstObject(query, entityMapper, params);
		} catch (Exception e) {
			notifyError(e, "Error querying for {}. Query: {}; Arguments: {}", entityName, query, params);
		}
		return null;
	}

	public final T queryForOptionalEntity(String query, Object... params) {
		try {
			Function<Object[], T> getter = cachedQueries.get(query);
			if (getter != null) {
				return cache.get(query, params, getter);
			}
			return queryForOptionalEntityNoCache(query, params);
		} catch (Exception e) {
			notifyError(e, "Error querying for {}. Query: {}; Arguments: {}", entityName, query, params);
		}
		return null;
	}

	private final T queryForOptionalEntityNoCache(String query, Object... params) {
		try {
			return db.queryForOptionalObject(query, entityMapper, params);
		} catch (Exception e) {
			notifyError(e, "Error querying for {}. Query: {}; Arguments: {}", entityName, query, params);
		}
		return null;
	}


	public final T queryForEntityNoExceptionHandling(String query, Object... params) {
		Function<Object[], T> getter = cachedQueries.get(query);
		if (getter != null) {
			T out = cache.get(query, params, getter);
			if (out == null) {
				throw new IllegalStateException(newMessage("Could not find " + entityName + ". Query: {}; Arguments: {}", query, params));
			}
		}
		return db.queryForObject(query, entityMapper, params);
	}

	public final T queryForEntity(String query, Object... params) {
		try {
			return queryForEntityNoExceptionHandling(query, params);
		} catch (EmptyResultDataAccessException e) {
			throw new IllegalStateException(newMessage("Could not find " + entityName + ". Query: {}; Arguments: {}", query, params), e);
		} catch (Exception e) {
			notifyError(e, "Error querying for {}. Query: {}; Arguments: {}", entityName, query, params);
		}
		return null;
	}

	public final List<T> queryForEntities(String query, Object... params) {
		try {
			return db.query(query, entityMapper, params);
		} catch (Exception e) {
			notifyError(e, "Error querying for {}. Query: {}; Arguments: {}", entityName, query, params);
		}
		return Collections.emptyList();
	}

	public void enableCacheFor(String query, Function<T, Object[]> keyToEvictGenerator) {
		cachedQueries.put(query, queryArgs -> queryForOptionalEntityNoCache(query, queryArgs));
		cacheEviction.put(query, keyToEvictGenerator);
		evictionParent.put(keyToEvictGenerator, query);
	}

	public void enableCacheFor(String query, Function<Object[], T> Function, Function<T, Object[]> keyToEvictGenerator) {
		cachedQueries.put(query, Function);
		cacheEviction.put(query, keyToEvictGenerator);
		evictionParent.put(keyToEvictGenerator, query);
	}

	public void clearCache() {
		cache.clear();
	}

	public boolean removeCachedEntry(SimpleCache.Key key) {
		return cache.remove(key) != null;
	}

	public Set<SimpleCache.Key> getCacheKeys() {
		return cache.keys();
	}
}
