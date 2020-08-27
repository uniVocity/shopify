package com.univocity.shopify.dao;

import com.univocity.shopify.*;
import com.univocity.shopify.model.db.*;
import org.springframework.beans.factory.annotation.*;
import org.testng.annotations.*;

import java.util.*;

import static org.testng.Assert.*;

public class VariantDaoTest extends BaseTest {

	@Autowired
	VariantDao variants;

	@Autowired
	ProductDao products;

	@Test
	public void testGetVariant() throws Exception {
		long shopId = shop1.id;
		List<Product> productList = products.getProducts(shopId);
		Product product = productList.get(0);

		List<Variant> variantsList = variants.getVariants(product);
		assertEquals(variantsList.size(), 2);

		Variant first = variantsList.get(0);
		Variant cached = variants.getVariant(first.getId(), first.getProductId(), shopId);
		assertTrue(first == cached);

		Variant cached2 = variants.getVariant(first.getId(), first.getProductId(), shopId);
		assertTrue(cached == cached2);
	}

	@Test
	public void testVariantCache() throws Exception {
		List<Product> productList = products.getProducts(shop2.id);
		Product product = productList.get(0);

		assertNull(variants.getVariant("lala", product.getId(), product.getShopId()));
		Variant v = new Variant();
		v.setDescription("lala");
		v.setProduct(product);
		v.setShopId(shop2.id);
		variants.persist(v);

		Variant created = variants.getVariant("lala", product.getId(), product.getShopId());
		assertNotNull(created);

		assertEquals(created.getDescription(), "lala");
		assertEquals(created.getProductId(), product.getId());
	}

	@Test
	public void testGetVariants() throws Exception {
		long shopId = shop1.id;
		List<Product> productList = products.getProducts(shopId);
		Product product = productList.get(0);

		List<Variant> variantsList = variants.getVariants(product);
		assertEquals(variantsList.size(), 2);
		for (Variant v : variantsList) {
			outputTester.println(v);
		}

//			outputTester.updateExpectedOutput();
		outputTester.printAndValidate();
	}

}