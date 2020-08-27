package com.univocity.shopify;

import com.univocity.shopify.dao.*;
import com.univocity.shopify.model.db.*;
import org.springframework.beans.factory.annotation.*;

import javax.annotation.*;
import java.nio.charset.*;
import java.util.*;

import static com.univocity.shopify.utils.Utils.*;


public class TestShopHelper {

	public final String name;
	public Long id;

	@Autowired
	public OrdersDao orders;

	@Autowired
	public ProductDao products;

	@Autowired
	public VariantDao variants;


	@Autowired
	public ShopDao shops;

	public TestShopHelper(String shopName) {
		this.name = shopName.toLowerCase();
	}

	@PostConstruct
	public void init() {
		if (shops.getShopIfExists(name) == null) {
			shops.registerShopToken(name, "token_" + name, false);
			Shop shop = shops.getShop(name);
			shop.setShopifyId(Math.abs(longHash(name)));
			shops.persist(shop);

			shops.activateShop(shop);
		}

		id = shops.getShopId(name);
	}

	public Product getProduct(Long productId) {
		return products.getProduct(productId, id);
	}

	public Variant getVariant(Product product, String description) {
		return variants.getVariant(product, description);
	}

	public List<Variant> getVariants(Product product) {
		return variants.getVariants(product);
	}


	public Product getProductDetails(Long shopifyId, String name, String variantDescription) {
		Product product = products.getProduct(name, id);
		if (product == null) {
			product = new Product();
			product.setName(name);
			product.setShopifyId(shopifyId);
			product.setShopId(id);
			products.insert(product);
		}

		if (variantDescription != null) {
			Variant variant = variants.getVariant(variantDescription, product.getId(), id);
			if (variant == null) {
				variant = new Variant();
				variant.setDescription(variantDescription);
				variant.setProduct(product);
				variant.setShopId(id);
				variants.insert(variant);
			}
		}

		return null;
	}

	public void processOrder(String json) {
		try {
			String jsonText = readTextFromResource("inputs/" + json, StandardCharsets.UTF_8);
			orders.processOrder(jsonText, name);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}


}
