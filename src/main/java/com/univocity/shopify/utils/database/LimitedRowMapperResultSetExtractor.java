package com.univocity.shopify.utils.database;

import org.springframework.jdbc.core.*;

public class LimitedRowMapperResultSetExtractor extends RowMapperResultSetExtractor {
	public LimitedRowMapperResultSetExtractor(RowMapper rowMapper) {
		super(rowMapper);
	}


}
