package com.univocity.shopify.model.db;


import com.univocity.shopify.model.db.core.*;

import java.sql.*;
import java.util.*;

import static com.univocity.shopify.utils.database.Converter.*;


public abstract class ImageEntity<T extends ShopifyEntity<T>> extends ShopifyEntity<T> {

	private Long imageId;
	private Image image;

	protected void populateMap(Map<String, Object> map) {
		map.put("image_id", getImageId());
		toMap(map);
	}

	protected abstract void toMap(Map<String, Object> map);

	public abstract String getName();

	@Override
	protected final void populateFromResultSet(ResultSet rs, int rowNum) throws SQLException {
		setImageId(readLong(rs, "image_id"));
		fromResultSet(rs, rowNum);
	}

	protected abstract void fromResultSet(ResultSet rs, int rowNum) throws SQLException;


	public Long getImageId() {
		return imageId;
	}

	public void setImageId(Long imageId) {
		this.imageId = imageId;
		this.image = idUpdated(image, imageId);
	}

	public Image getImage() {
		return image;
	}

	public void setImage(Image image) {
		this.image = image;
		this.imageId = objectUpdated(image);
	}

	public final boolean isDisabled() {
		return !isEnabled();
	}

	public abstract boolean isEnabled();

}
