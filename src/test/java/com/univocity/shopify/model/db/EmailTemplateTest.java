package com.univocity.shopify.model.db;

import org.testng.annotations.*;

import static org.testng.Assert.*;

public class EmailTemplateTest {

	@Test
	public void testTemplateProcessingWithPartialValues() {
		EmailTemplate t = EmailType.ORDER_PLACED.getDefaultTemplate();
		MessageRenderer r = t.renderer();

		r.setName("Jeronimo");
		r.setOrderId("#100");

		Shop shop = new Shop();
		shop.setShopName("mycrowsoft.com");
		r.setStoreName(shop);

		assertEquals(r.renderTitle(), "Your order #100 has been placed and is awaiting payment.");
		assertEquals(r.renderBody(), "" +
				"Dear Jeronimo,\r\n" +
				"\r\n" +
				"This link will remain valid while payment is pending. If you already paid please disconsider this e-mail.\r\n" +
				"\r\n" +
				"Kind regards,\r\n" +
				"mycrowsoft.com\r\n"
		);
	}

	@Test
	public void testTemplateProcessingWithAllValues() {
		EmailTemplate t = EmailType.ORDER_PLACED.getDefaultTemplate();
		MessageRenderer r = t.renderer();

		r.setExpirationDate(java.sql.Date.valueOf("2015-12-12"));
		r.setName("Jeronimo");
		r.setOrderId("#100");

		Shop shop = new Shop();
		shop.setShopName("mycrowsoft.com");
		r.setStoreName(shop);

		assertEquals(r.renderTitle(), "Your order #100 has been placed and is awaiting payment.");
		assertEquals(r.renderBody(), "" +
				"Dear Jeronimo,\r\n" +
				"\r\n" +
				"The purchase price is guaranteed until until: Dec 12, 2015.\r\n" +
				"\r\n" +
				"This link will remain valid while payment is pending. If you already paid please disconsider this e-mail.\r\n" +
				"\r\n" +
				"Kind regards,\r\n" +
				"mycrowsoft.com\r\n"
		);
	}
}