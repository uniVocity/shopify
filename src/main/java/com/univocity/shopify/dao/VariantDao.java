package com.univocity.shopify.dao;

import com.univocity.shopify.dao.base.*;
import com.univocity.shopify.model.db.*;
import com.univocity.shopify.model.shopify.*;
import com.univocity.shopify.utils.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;

import java.util.*;

/**
 * @author uniVocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 */
public class VariantDao extends ShopifyEntityDao<Variant> {

	private static final Logger log = LoggerFactory.getLogger(VariantDao.class);

	private static final String SELECT_VARIANT_BY_ID = "SELECT * from variant WHERE id = ? AND product_id = ? AND shop_id = ?";
	private static final String SELECT_VARIANT_BY_DESCRIPTION = "SELECT * from variant WHERE description = ? AND product_id = ? AND shop_id = ? AND deleted_at IS NULL";

	@Autowired
	ImageDao images;

	@Autowired
	ProductDao products;

	public VariantDao() {
		super("variant", true);

		enableCacheFor(SELECT_VARIANT_BY_ID, (Variant variant) -> new Object[]{variant.getId(), variant.getProductId(), variant.getShopId()});
		enableCacheFor(SELECT_VARIANT_BY_DESCRIPTION, (Variant variant) -> new Object[]{variant.getDescription(), variant.getProductId(), variant.getShopId()});
	}

	public Variant getVariant(Long variantId, Long product_id, Long shopId) {
		return queryForOptionalEntity(SELECT_VARIANT_BY_ID, variantId, product_id, shopId);
	}

	private Variant setProduct(Variant out, Product product) {
		if (out != null) {
			out.setProduct(product);
		}
		return out;
	}

	public Variant getVariant(Product product, String description) {
		Utils.notNull(product, "Product");
		Variant out = getVariant(description, product.getId(), product.getShopId());
		return setProduct(out, product);
	}


	public Variant getVariant(String description, String productName, Long shopId) {
		Product product = products.getProduct(productName, shopId);
		if (product == null) {
			return null;
		}
		Variant out = getVariant(description, product.getId(), shopId);
		return setProduct(out, product);
	}

	public Variant getVariant(String description, Long product_id, Long shopId) {
		if (description == null) {
			return null;
		}
		return queryForOptionalEntity(SELECT_VARIANT_BY_DESCRIPTION, description, product_id, shopId);
	}

	public Variant getVariant(Product product, Long variantId) {
		if (variantId == null) {
			return null;
		}
		Variant out = getVariant(variantId, product.getId(), product.getShopId());
		return setProduct(out, product);
	}

	public List<Variant> getVariants(Product product) {
		return getVariants(product.getId(), product.getShopId());
	}

	public long getVariantCount(Product product) {
		return getVariantCount(product.getId(), product.getShopId());
	}

	public long getVariantCount(Long productId, Long shopId) {
		return db.count("SELECT COUNT(*) from variant WHERE product_id = " + productId + " AND shop_id = " + shopId + " AND deleted_at IS NULL");
	}

	public List<Variant> getVariants(Long productId, Long shopId) {
		List<Long> variantIds = queryForIds("SELECT id from variant WHERE product_id = ? AND shop_id = ? AND deleted_at IS NULL", productId, shopId);
		List<Variant> out = new ArrayList<>(variantIds.size());

		for (Long variantId : variantIds) {
			Variant variant = getVariant(variantId, productId, shopId);
			if (variant != null) {
				out.add(variant);
			} else {
				log.warn("No variant with ID {} associated to product {}, shop {}", variantId, productId, shopId);
			}
		}

		return out;
	}

	public Variant persist(Product product, ShopifyLineItem shopifyLineItem, Long shopId) {
		Variant variant = queryForOptionalEntity("SELECT * FROM variant WHERE shop_id = ? AND shopify_id = ? AND product_id = ? AND deleted_at IS NULL", shopId, shopifyLineItem.variantId, product.getId());
		if (variant == null) {
			variant = new Variant(shopifyLineItem);
			variant.setShopId(shopId);
			variant.setProduct(product);
			persist(variant);
		} else if (variant.getProduct() == null) {
			variant.setProduct(product);
		}
		return variant;
	}

	public Variant persist(ProductVariant shopifyVariant, Product product) {
		Long shopId = product.getShopId();
		Variant variant = getByShopifyId(shopId, shopifyVariant.id);
		Image image = null;

		if (shopifyVariant.imageId != null) {
			if (variant != null && variant.getImage() != null) {
				if (variant.getImage().getShopifyId() != shopifyVariant.imageId) {
					image = images.getByShopifyId(shopId, shopifyVariant.imageId);
				}
			} else {
				image = images.getByShopifyId(shopId, shopifyVariant.imageId);
			}
		}
		if (image == null) {
			image = product.getImage();
			if (image == null) {
				image = images.getById(shopId, product.getImageId());
			}
		}

		if (variant == null) {
			variant = new Variant(shopifyVariant, shopId, product, image);
		} else {
			variant.setDescription(shopifyVariant.title);
			variant.setSku(shopifyVariant.sku);
			variant.setPrice(shopifyVariant.price);

			variant.setProduct(product);
			variant.setImage(image);
		}
		persist(variant);

		return variant;
	}


	@Override
	protected Variant newEntity() {
		return new Variant();
	}

	public Variant getVariantSafe(Product product, Long variantId) {
		Variant variant = null;
		if (product != null) {
			if (variantId != null) {
				variant = getVariant(product, variantId);
			}
		}
		return variant;
	}
}
