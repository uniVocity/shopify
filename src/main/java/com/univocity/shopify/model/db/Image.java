package com.univocity.shopify.model.db;


import com.univocity.shopify.model.db.core.*;
import com.univocity.shopify.model.shopify.*;

import java.sql.*;
import java.util.*;

import static com.univocity.shopify.utils.database.Converter.*;

/**
 * @author uniVocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 */
public class Image extends ShopifyEntity<Image> {
	private String url;
	private Integer height;
	private Integer width;

	public Image(){

	}
	public Image(ShopifyImage image, Long shopId){
		this.setShopifyId(image.id);
		this.setShopId(shopId);
		this.height = image.height;
		this.width = image.width;
		this.url = image.src;
	}

	@Override
	protected void populateMap(Map<String, Object> map) {
		map.put("height", getHeight());
		map.put("width", getWidth());
		map.put("url", getUrl());
	}

	@Override
	protected void populateFromResultSet(ResultSet rs, int rowNum) throws SQLException {
		setHeight(readInteger(rs, "height"));
		setWidth(readInteger(rs, "width"));
		setUrl(rs.getString("url"));
	}


	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Integer getHeight() {
		return height;
	}

	public void setHeight(Integer height) {
		this.height = height;
	}

	public Integer getWidth() {
		return width;
	}

	public void setWidth(Integer width) {
		this.width = width;
	}
}
