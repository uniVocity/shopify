package com.univocity.shopify.dao;

import com.univocity.shopify.dao.base.*;
import com.univocity.shopify.model.db.*;
import com.univocity.shopify.model.shopify.*;
import org.slf4j.*;

/**
 * @author uniVocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 */
public class ImageDao extends ShopifyEntityDao<Image> {

	private static final Logger log = LoggerFactory.getLogger(ImageDao.class);

	@Override
	protected Image newEntity() {
		return new Image();
	}

	public ImageDao() {
		super("image", true);
	}

	public void loadImage(ImageEntity entity) {
		try {
			if (entity.getImageId() != null) {
				Image image = getById(entity.getShopId(), entity.getImageId());
				entity.setImage(image);
			}
		} catch (Exception e) {
			log.error("Could not load image of " + entity, e);
		}
	}

	public Image persist(ShopifyImage shopifyImage, long shopId) {
		if (shopifyImage == null) {
			return null;
		}
		Image image = getByShopifyId(shopId, shopifyImage.id);
		if (image == null) {
			image = new Image(shopifyImage, shopId);
			persist(image);
		} else {
			image.setUrl(shopifyImage.src);
			image.setHeight(shopifyImage.height);
			image.setWidth(shopifyImage.width);
			persist(image);
		}

		return image;
	}
}