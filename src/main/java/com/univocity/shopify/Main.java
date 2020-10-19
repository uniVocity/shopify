package com.univocity.shopify;

import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.context.*;
import org.springframework.scheduling.annotation.*;
import org.springframework.web.servlet.config.annotation.*;

import javax.annotation.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author uniVocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 */
@SpringBootApplication
@EnableScheduling
public class Main implements WebMvcConfigurer {

	private static final Logger log = LoggerFactory.getLogger(Main.class);

	@Autowired
	private ApplicationContext context;

	public static final Set<String> endpoints = ConcurrentHashMap.newKeySet();

	private static ApplicationContext staticContext;


	public static ApplicationContext getApplicationContext() {
		return staticContext;
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/layout/**").addResourceLocations("classpath:/template/layout/")
				.setCachePeriod(0);
	}

	@PostConstruct
	public void init() {
		staticContext = context;
	}

	public static void main(String[] args) {
		SpringApplication.run(Main.class, args);
	}
}
