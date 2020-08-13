/*
 * Copyright (c) 2018 Univocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 *
 */

package com.univocity.shopify.utils.database;

import java.sql.*;

public interface ResultSetConsumer {
	void consume(ResultSet rs) throws SQLException;
}
