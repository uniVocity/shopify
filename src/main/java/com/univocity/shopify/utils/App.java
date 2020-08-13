package com.univocity.shopify.utils;

import org.springframework.beans.factory.annotation.*;

import javax.annotation.*;

public class App {

	@Autowired
	PropertyBasedConfiguration config;


	@PostConstruct
	public void initialize() {
		testingLocally = config.getBoolean("local.testing", false);
	}

	private boolean testingLocally;

	public final boolean isLive() {
		return !testingLocally;
	}

	public final boolean isTestingLocally() {
		return testingLocally;
	}
}
