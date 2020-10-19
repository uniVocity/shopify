package com.univocity.shopify;

import com.univocity.shopify.dao.*;
import com.univocity.test.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.test.context.*;
import org.springframework.test.context.testng.*;
import org.testng.annotations.*;

/**
 * Base test class for database dependent tests. Check src/test/resources/testng.xml see the sequence of tests being executed.
 *
 * @author uniVocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 */

@ContextConfiguration(classes = {TestServiceConfiguration.class})
public class BaseTest extends AbstractTestNGSpringContextTests {

	protected final OutputTester outputTester = new OutputTester(getClass(), "expected_outputs/", "UTf-8");

	@Autowired
	public TestShopHelper shop1;

	@Autowired
	public TestShopHelper shop2;

	@Autowired
	public TestShopHelper shopUnivocity;

	@Autowired
	public OrdersDao orders;

	@Autowired
	public ProductDao products;

	@Autowired
	public ShopDao shops;


	@Override
	@BeforeClass
	protected void springTestContextPrepareTestInstance() throws Exception {
		init();
		super.springTestContextPrepareTestInstance();
//		outputTester.setUpdateExpectedOutputs(true);
	}

	protected void init(){

	}

	@BeforeClass(dependsOnMethods = "springTestContextPrepareTestInstance")
	public void setUp() throws Exception {
		shop1.getProductDetails(5664741327002L, "Sock", 35919672869019L, "black");
		shop1.getProductDetails(5664741327002L, "Sock", 35919672869018L, "white");

		shop2.getProductDetails(5664741327002L, "Sock", 35919672869019L, "black");
		shop2.getProductDetails(5664741327002L, "Sock", 35919672869018L, "white");

		//process order files
		shop1.processOrder("order_created.json");
		shop2.processOrder("order_created.json");

	}
}
