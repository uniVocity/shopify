package com.univocity.shopify.dao.base;


import com.univocity.shopify.dao.*;
import com.univocity.shopify.utils.*;
import com.univocity.shopify.utils.database.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;

import java.sql.*;
import java.util.*;
import java.util.function.*;

/**
 * @author uniVocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 */
public abstract class BaseDao {

	private static final Logger log = LoggerFactory.getLogger(BaseDao.class);

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	protected ExtendedJdbcTemplate db;

	@Autowired
	protected PropertyBasedConfiguration config;

	@Autowired
	protected App app;

	@Autowired
	protected ShopDao shops;


	public boolean isDatabaseMySQL() {
		return app.isDatabaseMySQL();
	}

	protected final <R> R executeTransaction(Function<TransactionStatus, R> f) {
		return transactionTemplate.execute(f::apply);
	}

	public final List<Long> queryForIds(String query, Object... params) {
		try {
			return db.queryForList(query, params, Long.class);
		} catch (Exception e) {
			notifyError(e, "Error querying for list of IDs. Query: {}; Arguments: {}", query, params);
		}
		return Collections.emptyList();
	}

	protected final void notifyError(String msg, Object... args) {
		app.notifyError(msg, args);
	}

	protected final void notifyError(Exception e, String msg, Object... args) {
		app.notifyError(e, msg, args);
	}

	public static Timestamp now() {
		return new Timestamp(System.currentTimeMillis());
	}
}
