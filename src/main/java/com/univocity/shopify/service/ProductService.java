package com.univocity.shopify.service;

import com.univocity.shopify.controllers.api.*;
import com.univocity.shopify.dao.*;
import com.univocity.shopify.exception.*;
import com.univocity.shopify.model.db.*;
import com.univocity.shopify.model.shopify.*;
import com.univocity.shopify.utils.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;

import java.util.function.*;

import static com.univocity.shopify.dao.base.BaseDao.*;


public class ProductService {

	private static final Logger log = LoggerFactory.getLogger(ProductService.class);

	private static final SimpleCache<Product> transientProducts = new SimpleCache<>(10000);

	@Autowired
	ProductDao products;

	@Autowired
	VariantDao variants;

	@Autowired
	ImageDao images;

	@Autowired
	ShopDao shops;

	@Autowired
	ShopifyApiService shopifyApiService;

	@Autowired
	App app;

	public Product updateFromShopifyWebhook(String shopName, String json) {
		Long shopId = shops.getShopId(shopName);
		ShopifyProduct shopifyProduct = app.toObject(ShopifyProduct.class, json);

		Product product = products.getByShopifyId(shopId, shopifyProduct.id);
		if (product != null) {
			log.info("Processing product update webhook from shopify. Shop {}, Product ID {}", shopName, product.getId());
			return updateFromShopify(shopId, shopifyProduct, null);
		} else {
			transientProducts.remove(shopId, new Object[]{Long.valueOf(shopifyProduct.id)});
		}
		log.debug("Ignored product update webhook from shopify. Product not enabled. Shop {}, JSON {}", shopName, json);
		return null;
	}

	private Product updateFromShopify(Long shopId, ShopifyProduct shopifyProduct, Consumer<Product> productConsumer) {
		if (shopifyProduct.images != null) {
			for (ShopifyImage shopifyImage : shopifyProduct.images) {
				images.persist(shopifyImage, shopId);
			}
		}

		Image productImage;
		if (shopifyProduct.image != null) {
			productImage = images.persist(shopifyProduct.image, shopId);
		} else {
			productImage = null;
		}

		Product product = products.getByShopifyId(shopId, shopifyProduct.id);
		if (product == null) {
			product = new Product();
			product.setShopId(shopId);
			product.setShopifyId(shopifyProduct.id);
		}

		product.setName(shopifyProduct.title);
		product.setImage(productImage);

		if (productConsumer != null) {
			productConsumer.accept(product);
		}
		products.persist(product);

		if (shopifyProduct.variants != null) {
			for (ProductVariant variant : shopifyProduct.variants) {
				variants.persist(variant, product);
			}
		}

		return product;
	}

	public void setEnabled(boolean enable, Product product) {
		if (enable && product.isEnabled() || !enable && product.isDisabled()) {
			return;
		}

		product.setDisabledAt(enable ? null : now());
		this.persist(product);
		log.info((enable ? "Enabled " : "Disabled ") + product);
	}

	public Product getProductById(String shopName, String productId) {
		return products.getById(shopName, Long.valueOf(productId));
	}

	public Product getProductById(Long shopId, Long productId) {
		return products.getById(shopId, productId);
	}

	public Product getProductByShopifyIdNoCache(String shopName, String shopifyId) {
		return getProductByShopifyIdNoCache(shops.getShopId(shopName), Long.valueOf(shopifyId));
	}

	public Product getProductByShopifyIdNoCache(Long shopId, Long shopifyId) {
		log.debug("Product {} of shop {} not found. Loading from shopify.", shopifyId, shopId);

		ShopifyProduct shopifyProduct = shopifyApiService.getProduct(shops.getShopName(shopId), String.valueOf(shopifyId), null);
		if (shopifyProduct == null) {
			log.error("Could not load details of product {} from shop {}", shopifyId, shopId);
			throw new ApplicationStateException("Error loading product details");
		}

		return new Product(shopifyProduct, shopId);
	}

	public Product getProductByShopifyId(Long shopId, Long shopifyId) {
		Product out = products.getByShopifyId(shopId, shopifyId);
		if (out == null) {
			out = transientProducts.get(shopId, new Object[]{shopifyId});

			if (out != null) {
				return out;
			}

			out = getProductByShopifyIdNoCache(shopId, shopifyId);

			transientProducts.put(shopId, new Object[]{shopifyId}, out);
		}

		if(out.getImage() == null && out.getImageId() != null){
			Image image = images.getById(shopId, out.getImageId());
			out.setImage(image);
		}

		return out;
	}

	public Product getProductByShopifyId(String shopName, String shopifyId) {
		return getProductByShopifyId(shops.getShopId(shopName), Long.valueOf(shopifyId));
	}

	private void persistImage(ImageEntity entity) {
		Image image = entity.getImage();
		if (image != null) {
			images.persist(image);
			entity.setImage(image);
		}
	}

	public void persist(Product product) {
		persistImage(product);

		products.persist(product);
		for (Variant variant : product.getVariants()) {
			variant.setProduct(product); // update product id.
			persistImage(variant);
			variants.persist(variant);
		}

		transientProducts.remove(product.getShopId(), new Object[]{product.getShopifyId()});
	}

	public void persist(Variant variant) {
		if (variant.getProduct().getId() == null) {
			persist(variant.getProduct());
		} else {
			if (variant.getImage() != null && variant.getImage().getId() == null) {
				images.persist(variant.getImage());
			}
			variants.persist(variant);
		}
	}

	public Variant getVariantByShopifyId(String shopName, String shopifyId) {
		Long variantId = Long.valueOf(shopifyId);
		Variant variant = variants.getByShopifyId(shopName, variantId);
		Product product;
		if (variant == null) {
			ProductVariant shopifyVariant = shopifyApiService.getVariant(shopName, shopifyId, null);
			if (shopifyVariant == null) {
				log.error("Could not load details of variant {} from shop {}", variantId, shopName);
				throw new ApplicationStateException("Error loading variant details");
			}
			Long shopId = shops.getShopId(shopName);
			product = getProductByShopifyId(shopId, shopifyVariant.productId);

			variant = product.getVariants().stream()
					.filter(v -> variantId.equals(v.getShopifyId()))
					.findFirst()
					.orElseThrow(() -> {
						log.error("Could not load details of variant {} from shop {}", variantId, shopName);
						return new ApplicationStateException("Error loading variant details");
					});
		} else {
			product = products.getProduct(variant.getProductId(), variant.getShopId());
		}

		if (variant.getImage() == null && variant.getImageId() != null) {
			Image image = images.getById(variant.getShopId(), variant.getImageId());
			variant.setImage(image);
		}

		variant.setProduct(product);
		return variant;
	}

	public Variant getVariantById(Long shopId, Long varId) {
		return variants.getById(shopId, varId);
	}
}
