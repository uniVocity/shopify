package com.univocity.shopify.server;
import com.univocity.shopify.*;
import com.univocity.shopify.dao.*;
import com.univocity.shopify.utils.*;
import com.univocity.shopify.utils.database.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.*;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.test.context.testng.*;
import org.springframework.web.client.*;
import org.testng.annotations.*;

import static org.testng.Assert.*;

@Import({ServerTestConfig.class})
public class ServerTest extends AbstractTestNGSpringContextTests {

	static { //Allows connection to localhost.
		javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(
				(hostname, sslSession) -> {
					if (hostname.equals("localhost")) {
						return true;
					}
					return false;
				});

	}

	@Autowired
	ApplicationContext testContext;

	@Autowired
	ShopDao shops;

	@Autowired
	TestShopHelper shop1;

	@Autowired
	OrdersDao orderDao;

	@Autowired
	VariantDao variantDao;

	@Autowired
	ProductDao productDao;

	@Autowired
	DbSetup dbSetup;

	@Autowired
	RestTemplate restTemplate;

	@Autowired
	CustomerDao customerDao;

	@Autowired
	App app;

	@Autowired
	SystemMessageDao systemMessageDao;

	@Override
	@BeforeClass
	protected void springTestContextPrepareTestInstance() throws Exception {
		super.springTestContextPrepareTestInstance();
		init();
		Main.main(new String[]{});
	}


	protected void init() {
		dbSetup.dropTables();
		shops.clearCaches();

		ApplicationConfiguration.setupDb(dbSetup);

		shop1.init();
	}

	@Test
	public void endpointTest() {
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.ORIGIN, "https://localhost:8788");
		HttpEntity<?> requestEntity = new HttpEntity(headers);

		//endpoint doesn't exist
		ResponseEntity<String> entity = restTemplate.exchange("https://localhost:8788/invalid/thing", HttpMethod.POST, requestEntity, String.class);
		assertEquals(HttpStatus.NOT_FOUND, entity.getStatusCode());
		assertNull(entity.getBody(), null);

		entity = restTemplate.exchange("https://localhost:8788/preferences?shop=shop1", HttpMethod.POST, requestEntity, String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertNotNull(entity.getBody());
	}
}


