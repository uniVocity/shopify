package com.univocity.shopify;

import com.univocity.shopify.controllers.api.*;
import com.univocity.shopify.utils.*;
import org.springframework.beans.factory.config.*;
import org.springframework.context.annotation.*;
import org.springframework.context.support.*;
import org.springframework.web.context.*;

import java.util.*;

/**
 * @author uniVocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 */
@Configuration()
@Import({AuthenticationService.class, ApplicationConfiguration.class, ShopifyApiService.class})
public class TestServiceConfiguration {


	@Bean
	TestShopHelper shop1() {
		return new TestShopHelper("Shop1");
	}

	@Bean(name = "shop2")
	TestShopHelper shop2() {
		return new TestShopHelper("Shop2");
	}

	@Bean
	TestShopHelper shopUnivocity() {
		return new TestShopHelper("univocity");
	}

	@Bean
	App getApp() {
		return new AppMock();
	}

	@Bean
	public CustomScopeConfigurer customScopeConfigurer() {
		CustomScopeConfigurer scopeConfigurer = new CustomScopeConfigurer();

		HashMap<String, Object> scopes = new HashMap<>();
		scopes.put(WebApplicationContext.SCOPE_REQUEST, new SimpleThreadScope());
		scopes.put(WebApplicationContext.SCOPE_SESSION, new SimpleThreadScope());
		scopeConfigurer.setScopes(scopes);

		return scopeConfigurer;
	}

	@Bean(name = "chargingShop")
	TestShopHelper getChargingShop(){
		return new TestShopHelper("charging");
	}

	@Bean
	ShopifyWebhookListener webhookListener(){
		return new ShopifyWebhookListener();
	}


	public TestServiceConfiguration() {

	}
}
