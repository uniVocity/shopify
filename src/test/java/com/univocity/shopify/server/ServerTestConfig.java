package com.univocity.shopify.server;

import com.univocity.shopify.*;
import com.univocity.shopify.controllers.api.*;
import com.univocity.shopify.utils.*;
import org.springframework.context.annotation.*;

@Configuration()
@Import({AuthenticationService.class, ApplicationConfiguration.class, ShopifyApiService.class})
public class ServerTestConfig {

	@Bean
	TestShopHelper shop1() {
		return new TestShopHelper("Shop1");
	}

	@Bean
	App getApp() {
		return new AppMock();
	}

	public ServerTestConfig() {

	}
}
