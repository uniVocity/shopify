package com.univocity.shopify.dao;


import com.univocity.shopify.*;
import com.univocity.shopify.model.db.*;
import com.univocity.shopify.model.shopify.*;
import org.springframework.beans.factory.annotation.*;
import org.testng.annotations.*;

import java.util.*;

import static org.testng.Assert.*;

public class CustomerDaoTest extends BaseTest {

	@Autowired
	CustomerDao customers;

	@DataProvider
	public Object[][] shopProvider() {
		return new Object[][]{
				{"Shop1"},
				{"Shop2"},
		};
	}

	@Test(dataProvider = "shopProvider")
	public void testPersistedCustomers(String shop) throws Exception {
		List<Customer> customerList = customers.getCustomers(shop);
		assertEquals(customerList.size(), 1);

		outputTester.println(customerList.get(0));

//		outputTester.updateExpectedOutputUsingArgs(shop);
		outputTester.printAndValidateUsingArgs(shop);
	}

	@Test
	public void testNoDuplicateCustomersCreated() throws Exception {
		List<Customer> customerList = customers.getCustomers(shop1.name);
		assertEquals(customerList.size(), 1);

		for (Customer customer : customerList) {
			ShopifyCustomer c = new ShopifyCustomer();
			c.id = customer.getShopifyId();
			c.firstName = customer.getFirstName();
			c.lastName = customer.getLastName();
			c.email = customer.getEmail();

			customers.persist(c, shop1.name);
		}

		customerList = customers.getCustomers(shop1.name);
		assertEquals(customerList.size(), 1);
	}

}