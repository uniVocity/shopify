package com.univocity.shopify.dao;

import com.univocity.shopify.*;
import com.univocity.shopify.model.db.*;
import org.testng.annotations.*;

import java.util.*;

import static org.testng.Assert.*;

public class OrdersDaoTest extends BaseTest {

	@DataProvider
	public Object[][] shopProvider() {
		return new Object[][]{
				{"Shop1"},
				{"Shop2"},
		};
	}

	@Test(dataProvider = "shopProvider")
	public void testPersistedOrders(String shop) throws Exception {
		List<Order> orderList = orders.getOrders(shop);
		assertEquals(orderList.size(), 1);

		outputTester.println(orderList.get(0));
//		outputTester.updateExpectedOutputUsingArgs(shop);
		outputTester.printAndValidateUsingArgs(shop);
	}

	@Test
	public void testNoDuplicateOrdersCreated() throws Exception {
		shop1.processOrder("order_created.json");
		shop1.processOrder("order_created.json");
		List<Order> orderList = orders.getOrders(shop1.name);
		assertEquals(orderList.size(), 1);
	}

	@Test
	public void testGetOrderById() {
		List<Order> orderList = orders.getOrders(shop1.name);

		assertEquals(orderList.size(), 1);

		Order order = orderList.get(0);

		Order firstCopy = orders.getById(shop1.name, order.getId());
		assertEquals(order.toString(), firstCopy.toString());

		assertNull(orders.getOptionalEntityById(shop2.name, order.getId()));
	}

	@Test
	void testGetCustomerOrders() {
		List<Order> orderList = orders.getOrders(shop1.name);

		assertEquals(orderList.size(), 1);

		Order order = orderList.get(0);

		orderList = orders.getCustomerOrders(shop1.name, order.getCustomerId());
		assertEquals(orderList.size(), 1);
		assertEquals(order.toString(), orderList.get(0).toString());

		assertTrue(orders.getCustomerOrders(shop2.name, order.getCustomerId()).isEmpty());
	}

}
