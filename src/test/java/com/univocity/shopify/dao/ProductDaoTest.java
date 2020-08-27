package com.univocity.shopify.dao;


import com.univocity.shopify.*;
import com.univocity.shopify.exception.*;
import com.univocity.shopify.model.db.*;
import org.springframework.beans.factory.annotation.*;
import org.testng.annotations.*;

import java.util.*;

import static org.testng.Assert.*;

public class ProductDaoTest extends BaseTest {

	@Autowired
	ProductDao products;

 	@Test
	public void testGetProducts() throws Exception {
		List<Product> productList = products.getProducts(shop1.id);
		assertEquals(productList.size(), 1);
		for (Product p : productList) {
			outputTester.println(p);
		}

//		outputTester.updateExpectedOutput();
		outputTester.printAndValidate();
	}

	@Test
	public void testGetProduct() throws Exception {
		long shopId = shop1.id;
		List<Product> productList = products.getProducts(shopId);
		Product first = productList.get(0);

		Product found = products.getProduct(first.getId(), shopId);
		assertNotNull(found);
		assertFalse(first == found);

		//tests cache
		Product foundAgain = products.getProduct(first.getId(), shopId);
		assertTrue(found == foundAgain);
	}


	@Test
	public void testInsertProduct() {
		final String NAME = "testInsertProduct";

		assertNull(products.getProduct(NAME, shop2.id));

		Product newProduct = new Product();
		newProduct.setName(NAME);
		try {
			newProduct.setShopId(shops.getShopId("ashop"));
		} catch (ValidationException e) {
			//shop doesn't exist
			newProduct.setShopId(shop2.id);
		}
		products.persist(newProduct);

		assertNull(products.getProduct(NAME, shop1.id));
		Product product = products.getProduct(NAME, shop2.id);
		assertNotNull(product);
		assertEquals(product.getName(), NAME);

	}
}