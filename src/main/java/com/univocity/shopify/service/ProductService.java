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

	//sample json: {"id":5664741327002,"title":"Sock","body_html":"","vendor":"cardano-integration","product_type":"","created_at":"2020-08-26T18:19:38-04:00","handle":"sock","updated_at":"2020-08-26T20:01:20-04:00","published_at":"2020-08-26T18:19:39-04:00","template_suffix":"","published_scope":"web","tags":"","admin_graphql_api_id":"gid:\/\/shopify\/Product\/5664741327002","variants":[{"id":35919672869018,"product_id":5664741327002,"title":"white","price":"1.00","sku":"s1","position":1,"inventory_policy":"deny","compare_at_price":null,"fulfillment_service":"manual","inventory_management":"shopify","option1":"white","option2":null,"option3":null,"created_at":"2020-08-26T18:19:38-04:00","updated_at":"2020-08-26T18:19:38-04:00","taxable":true,"barcode":"","grams":25,"image_id":null,"weight":25.0,"weight_unit":"g","inventory_item_id":37878819029146,"inventory_quantity":100,"old_inventory_quantity":100,"requires_shipping":true,"admin_graphql_api_id":"gid:\/\/shopify\/ProductVariant\/35919672869018"},{"id":35919672901786,"product_id":5664741327002,"title":"black","price":"1.00","sku":"s2","position":2,"inventory_policy":"deny","compare_at_price":null,"fulfillment_service":"manual","inventory_management":"shopify","option1":"black","option2":null,"option3":null,"created_at":"2020-08-26T18:19:38-04:00","updated_at":"2020-08-26T19:59:17-04:00","taxable":true,"barcode":"","grams":25,"image_id":null,"weight":25.0,"weight_unit":"g","inventory_item_id":37878819061914,"inventory_quantity":100,"old_inventory_quantity":100,"requires_shipping":true,"admin_graphql_api_id":"gid:\/\/shopify\/ProductVariant\/35919672901786"}],"options":[{"id":7224567398554,"product_id":5664741327002,"name":"Color","position":1,"values":["white","black"]}],"images":[{"id":18784560251034,"product_id":5664741327002,"position":1,"created_at":"2020-08-26T18:19:40-04:00","updated_at":"2020-08-26T18:19:40-04:00","alt":null,"width":400,"height":400,"src":"https:\/\/cdn.shopify.com\/s\/files\/1\/0462\/5635\/7530\/products\/Valour-Sport-3-pack-cotton-school-socks-white-roll-top-sock_400x_8f8363ff-e376-4025-a00b-382b4d6b4219.jpg?v=1598480380","variant_ids":[],"admin_graphql_api_id":"gid:\/\/shopify\/ProductImage\/18784560251034"}],"image":{"id":18784560251034,"product_id":5664741327002,"position":1,"created_at":"2020-08-26T18:19:40-04:00","updated_at":"2020-08-26T18:19:40-04:00","alt":null,"width":400,"height":400,"src":"https:\/\/cdn.shopify.com\/s\/files\/1\/0462\/5635\/7530\/products\/Valour-Sport-3-pack-cotton-school-socks-white-roll-top-sock_400x_8f8363ff-e376-4025-a00b-382b4d6b4219.jpg?v=1598480380","variant_ids":[],"admin_graphql_api_id":"gid:\/\/shopify\/ProductImage\/18784560251034"}}
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
