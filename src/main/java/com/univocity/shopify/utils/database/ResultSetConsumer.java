

package com.univocity.shopify.utils.database;

import java.sql.*;

public interface ResultSetConsumer {
	void consume(ResultSet rs) throws SQLException;
}
