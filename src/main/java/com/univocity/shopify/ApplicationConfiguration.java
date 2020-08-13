package com.univocity.shopify;



import com.univocity.shopify.controllers.*;
import com.univocity.shopify.utils.*;
import com.zaxxer.hikari.*;

import org.slf4j.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.datasource.*;
import org.springframework.scheduling.annotation.*;
import org.springframework.scheduling.concurrent.*;
import org.springframework.transaction.support.*;
import org.springframework.web.client.*;

import javax.sql.*;


/**
 * @author uniVocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 */
@Configuration
@PropertySource("classpath:/config/application.properties")
@EnableScheduling
public class ApplicationConfiguration {

	private static final Logger log = LoggerFactory.getLogger(ApplicationConfiguration.class);

	/*
	 * GENERAL CONFIGS
	 */
//	@Bean
//	Utils getUtils() {
//		Utils utils = new Utils();
//		return utils;
//	}

	/*
	 * WEBSERVICE RELATED CONFIGS
	 */
	@Bean
	RestTemplate restTemplate() {
		RestTemplate out = new RestTemplate();
		out.setErrorHandler(new ApplicationResponseErrorHandler());
		out.getMessageConverters().add(0, new ShopifyMessageConverter());

		return out;
	}

	@Bean
	PropertyBasedConfiguration config() {
		return new PropertyBasedConfiguration("config/application.properties");
	}

	@Bean
	DataSource dataSource() {
		PropertyBasedConfiguration config = config();
		try {
			Class.forName(config.getProperty("database.driver"));
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		HikariConfig dsConfig = new HikariConfig();
		dsConfig.setJdbcUrl(config.getProperty("database.url"));
		dsConfig.setUsername(config.getProperty("database.user"));
		dsConfig.setPassword(config.getProperty("database.password"));
		dsConfig.addDataSourceProperty("cachePrepStmts", "true");
		dsConfig.addDataSourceProperty("prepStmtCacheSize", "250");
		dsConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

		DataSource dataSource = new HikariDataSource(dsConfig);
		return dataSource;
	}

	@Bean
	JdbcTemplate db() {
		JdbcTemplate out = new JdbcTemplate(dataSource());
		return out;
	}

	@Bean
	TransactionTemplate transactionTemplate() {
		return new TransactionTemplate(new DataSourceTransactionManager(dataSource()));
	}

	@Bean(destroyMethod = "shutdown")
	public ThreadPoolTaskScheduler taskScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(3);
		scheduler.setDaemon(true);
		return scheduler;
	}

}
