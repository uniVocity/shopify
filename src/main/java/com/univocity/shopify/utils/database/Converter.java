package com.univocity.shopify.utils.database;

import java.sql.*;

public interface Converter<T> {

	T convert(Object value);


	Converter<Long> longConverter = value -> {
		if (value == null) {
			return null;
		}
		return ((Number) value).longValue();
	};

	 Converter<Integer> integerConverter = value -> {
		 if (value == null) {
			 return null;
		 }
		 return ((Number) value).intValue();
	 };

	Converter<Boolean> booleanConverter = value -> {
		if (value == null) {
			return null;
		}
		if (value instanceof Boolean) {
			return (Boolean) value;
		}
		return Boolean.valueOf(value.toString());
	};


  Converter<Character> charConverter = value -> {
	  if (value == null) {
		  return null;
	  }
	  if (value instanceof Character) {
		  return (Character) value;
	  }
	  String str = value.toString();
	  if (str.isEmpty()) {
		  return null;
	  }

	  return str.charAt(0);
  };

	static Long readLong(ResultSet rs, String column) throws SQLException {
		return longConverter.convert(rs.getObject(column));
	}

	static Integer readInteger(ResultSet rs, String column) throws SQLException {
		return integerConverter.convert(rs.getObject(column));
	}

	static boolean readBoolean(ResultSet rs, String column, boolean defaultValue) throws SQLException {
		Boolean result = booleanConverter.convert(rs.getObject(column));
		if(result == null){
			return defaultValue;
		}
		return result;
	}

	static boolean readBoolean(ResultSet rs, String column) throws SQLException {
		return readBoolean(rs, column, false);
	}

	static Character readChar(ResultSet rs, String column) throws SQLException {
		return charConverter.convert(rs.getObject(column));
	}

}