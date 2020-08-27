package com.univocity.shopify.utils.database;

import com.univocity.shopify.utils.*;
import org.apache.commons.lang3.*;
import org.springframework.dao.*;
import org.springframework.dao.support.*;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.support.*;

import javax.sql.*;
import java.sql.*;
import java.util.*;

public class ExtendedJdbcTemplate extends JdbcTemplate {

	public ExtendedJdbcTemplate() {
	}

	public ExtendedJdbcTemplate(DataSource dataSource) {
		super(dataSource);
	}

	public ExtendedJdbcTemplate(DataSource dataSource, boolean lazyInit) {
		super(dataSource, lazyInit);
	}

	public Map<String, Object> queryForOptionaLMap(String sql) throws DataAccessException {
		try {
			return queryForMap(sql);
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	public <T> T queryForOptionalObject(String sql, Object[] args, Class<T> requiredType) throws DataAccessException {
		try {
			return queryForObject(sql, args, getSingleColumnRowMapper(requiredType));
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	public <T> T queryForOptionalObject(String sql, Class<T> requiredType) throws DataAccessException {
		try {
			return queryForObject(sql, getSingleColumnRowMapper(requiredType));
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	public <T> T queryForFirstObject(String sql, RowMapper<T> rowMapper, Object... args) throws DataAccessException {
		RowMapperResultSetExtractor<T> extractor = new RowMapperResultSetExtractor<T>(rowMapper, 1) {
			@Override
			public List<T> extractData(ResultSet rs) throws SQLException {
				List<T> results = new ArrayList<T>(1);
				if (rs.next()) {
					results.add(rowMapper.mapRow(rs, 0));
				}
				return results;
			}
		};
		List<T> results = query(sql, args, extractor);
		if (results.isEmpty()) {
			return null;
		}
		return results.get(0);
	}

	public <T> T queryForOptionalObject(String sql, RowMapper<T> rowMapper, Object... args) throws DataAccessException {
		List<T> results = query(sql, args, new RowMapperResultSetExtractor<T>(rowMapper, 1));
		if (results.isEmpty()) {
			return null;
		}
		return DataAccessUtils.requiredSingleResult(results);
	}

	public Number insertReturningKey(final String table, Map<String, Object> data) {
		return insertReturningKey(table, null, data);
	}

	public Number insertReturningKey(final String table, String generatedKeyColumn, Map<String, Object> data) {
		String insert = generateInsertStatement(table, data.keySet().toArray(new String[0]));

		KeyHolder holder = insertReturningKeys(insert, data.values().toArray(new Object[0]));
		return getGeneratedKey(holder, generatedKeyColumn);
	}

	private Number getGeneratedKey(KeyHolder holder, String column) {
		Map<String, Object> keys = holder.getKeys();
		if (keys == null || keys.isEmpty()) {
			return null;
		}
		if (column == null || keys.size() == 1) {
			return holder.getKey();
		} else {
			Number out = (Number) keys.get(column);
			if (out == null && !keys.containsKey(column)) {
				throw new IllegalArgumentException("Unknown generated column name '" + column + "'. Available column names are: " + keys.keySet());
			}
			return out;
		}
	}

	public int insert(final String insert, Object... args) {
		final PreparedStatementSetter pss = newArgPreparedStatementSetter(args);
		return update(new PreparedStatementCreator() {
			@Override
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(insert);
				pss.setValues(ps);
				return ps;
			}
		});
	}

	public int insert(final String table, Map<String, Object> data) {
		String insert = generateInsertStatement(table, data.keySet().toArray(new String[0]));
		return update(insert, data.values().toArray(new Object[0]));
	}

	public int update(final String table, Map<String, Object> data, LinkedHashSet<String> columnsToMatch) {
		return update(table, data, columnsToMatch, null);
	}

	public int update(final String table, Map<String, Object> data, LinkedHashSet<String> columnsToMatch, String additionalCriteria) {
		Object[][] selection = Utils.getValuesAndSelection(data, columnsToMatch);
		String updateStatement = createUpdateStatement(table, selection[0], data.size() - columnsToMatch.size(), additionalCriteria);

		return update(updateStatement, selection[1]);
	}

	public Number insertReturningKey(final String insert, Object[] args) {
		return insertReturningKey(insert, null, args);
	}

	public Number insertReturningKey(final String insert, String column, Object[] args) {
		return getGeneratedKey(insertReturningKeys(insert, args), column);
	}

	private KeyHolder insertReturningKeys(final String insert, Object[] args) {
		KeyHolder holder = new GeneratedKeyHolder();

		final PreparedStatementSetter pss = newArgPreparedStatementSetter(args);

		try {
			update(con -> {
				PreparedStatement ps = con.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS);
				pss.setValues(ps);
				return ps;
			}, holder);
		} catch (Exception ex) {
			throwUpdateError(ex, insert, args);
		} finally {
			if (pss instanceof ParameterDisposer) {
				((ParameterDisposer) pss).cleanupParameters();
			}
		}

		return holder;
	}

	private static DataUpdateException throwUpdateError(Throwable ex, String sql, Object[] args) {
		if (ex instanceof DataUpdateException) {
			throw (DataUpdateException) ex;
		}
		throw new DataUpdateException(sql, args, ex);
	}


	public void query(final String query, final ResultSetConsumer consumer) {
		execute((StatementCallback<Object>) stmt -> {
			ResultSet rs = stmt.executeQuery(query);
			try {
				consumer.consume(rs);
			} finally {
				rs.close();
			}
			return null;
		});
	}

	public long count(String sql, Object... args) {
		Number result = queryForOptionalObject(sql, args, Number.class);
		return result.longValue();
	}


	public static String generateInsertStatement(String tableName, String... columns) {
		StringBuilder out = new StringBuilder();
		out.append("insert into ").append(tableName);
		if (columns.length > 0) {
			out.append(" (");
			Utils.concatenate(out, ", ", columns);
			out.append(") values (");
			appendValuePlaceHolderString(out, columns.length);
			out.append(')');
		}
		return out.toString();
	}

	private static void appendValuePlaceHolderString(StringBuilder out, int columnCount) {
		if (columnCount <= 0) {
			return;
		}
		while (columnCount-- > 0) {
			out.append('?').append(',');
		}
		out.deleteCharAt(out.length() - 1);

	}

	public static String createUpdateStatement(String tableName, Object[] columns, int matchingStart, String additionalCriteria) {

		StringBuilder out = new StringBuilder();

		out.append("update ").append(tableName);

		out.append(" set ");
		Utils.concatenate(out, "=?,", matchingStart, columns);
		out.append("=?");

		String matchingString = createWhere(columns, matchingStart, columns.length, additionalCriteria);
		out.append(matchingString);


		return out.toString();
	}

	public static String createUpdateStatement(String tableName, String[] columnsToUpdate, String[] columnsToMatch) {
		return createUpdateStatement(tableName, columnsToUpdate, columnsToMatch, null);
	}

	public static String createUpdateStatement(String tableName, String[] columnsToUpdate, String[] columnsToMatch, String additionalCriteria) {
		StringBuilder out = new StringBuilder(createUpdateStatement(tableName, columnsToUpdate));
		String matchingString = createWhere(columnsToMatch, additionalCriteria);
		out.append(matchingString);
		return out.toString();
	}

	public static String createUpdateStatement(String tableName, String[] columnsToUpdate) {
		StringBuilder out = new StringBuilder();
		out.append("update ").append(tableName);
		if (columnsToUpdate.length > 0) {
			out.append(" set ");
			Utils.concatenate(out, "=?,", columnsToUpdate);
			out.append("=?");
		}
		return out.toString();
	}

	public static String createWhere(String[] columnsToMatch) {
		return createWhere(columnsToMatch, null);
	}

	public static String createWhere(String[] columnsToMatch, String additionalCriteria) {
		return createWhere(columnsToMatch, 0, columnsToMatch.length, additionalCriteria);
	}

	public static String createWhere(Object[] columnsToMatch, int from, int to, String additionalCriteria) {
		if (ArrayUtils.isEmpty(columnsToMatch)) {
			return "";
		}
		StringBuilder out = new StringBuilder(" where ");

		final String andKeyword = " and ";

		for (int i = from; i < to; i++) {
			Object column = columnsToMatch[i];
			out.append(column).append("=?");
			out.append(andKeyword);
		}

		if (StringUtils.isNotBlank(additionalCriteria)) {
			out.append(additionalCriteria);
		} else {
			int start = out.length() - andKeyword.length();
			int end = out.length();
			if (end > 0) {
				out.delete(start, end);
			}
		}

		return out.toString();
	}

	public static String createDeleteStatement(String tableName, String[] columnsToMatch) {
		StringBuilder out = new StringBuilder();

		out.append("delete from ").append(tableName);
		out.append(createWhere(columnsToMatch));
		return out.toString();
	}
}
