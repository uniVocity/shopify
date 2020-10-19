package com.univocity.shopify;


import com.univocity.shopify.controllers.api.*;
import com.univocity.shopify.dao.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.context.event.*;
import org.springframework.context.*;
import org.springframework.stereotype.*;

@Component
public class ApplicationReadyListener implements ApplicationListener<ApplicationReadyEvent> {

	private static final Logger log = LoggerFactory.getLogger(ApplicationReadyListener.class);

	@Autowired
	ShopDao shops;

	@Autowired
	ShopifyApiService shopifyApiService;

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		log.info("Updating webhook registrations if needed.");
		shops.getShops().forEach(shopifyApiService::updateShopWebooks);
	}

}