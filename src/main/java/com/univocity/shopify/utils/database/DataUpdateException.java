package com.univocity.shopify.utils.database;

import org.springframework.dao.*;

import java.util.*;

/**
 * @author Univocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 */
public class DataUpdateException extends DataAccessException {

	public DataUpdateException(String sql, Object[] args, Throwable cause) {
		super(buildMessage(sql, args), cause);
	}

	private static String buildMessage(String sql, Object[] args) {
		String message = "\n----[ Error performing update ]---- \n\t" + sql + "\nWith values: \n\t" + Arrays.toString(args) + "\n------------------------------------\n";
		return message;
	}
}
