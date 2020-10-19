package com.univocity.shopify.dao;


import com.univocity.shopify.*;
import com.univocity.shopify.model.db.*;
import org.springframework.beans.factory.annotation.*;
import org.testng.annotations.*;

import java.util.*;

public class LineItemDaoTest extends BaseTest {

	@Autowired
	LineItemDao lineItems;

	@Test
	public void testPersistLineItems(){
		List<Order> orderList = orders.getOrders(shop1.name);
		for (Order order : orderList) {
			List<LineItem> lineItemList = lineItems.loadLineItems(order.getId(), shop1.name);

			outputTester.println("Items of order #" + order.getShopifyOrderNumber());
			for (LineItem item : lineItemList) {
				outputTester.println(item);
			}
		}

//		outputTester.updateExpectedOutput();
		outputTester.printAndValidate();
	}

}