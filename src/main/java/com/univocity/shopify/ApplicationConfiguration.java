package com.univocity.shopify;


import com.univocity.shopify.controllers.*;
import com.univocity.shopify.dao.*;
import com.univocity.shopify.email.*;
import com.univocity.shopify.service.*;
import com.univocity.shopify.utils.*;
import com.univocity.shopify.utils.database.*;
import com.zaxxer.hikari.*;
import org.apache.commons.io.*;
import org.slf4j.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.datasource.*;
import org.springframework.scheduling.annotation.*;
import org.springframework.scheduling.concurrent.*;
import org.springframework.transaction.support.*;
import org.springframework.web.client.*;

import javax.sql.*;
import java.io.*;


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

	@Bean
	PropertyBasedConfiguration config() {
		return new PropertyBasedConfiguration("config/application.properties");
	}

	@Bean
	App getApp() {
		App app = new App();
		return app;
	}

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

	/*
	 * DAOs
	 */
	@Bean
	ShopDao shops() {
		return new ShopDao();
	}

	@Bean
	CustomerDao customers() {
		return new CustomerDao();
	}

	@Bean
	OrdersDao orders() {
		return new OrdersDao();
	}

	@Bean
	LineItemDao lineItems() {
		return new LineItemDao();
	}

	@Bean
	ProductDao products() {
		return new ProductDao();
	}


	@Bean
	ImageDao images() {
		return new ImageDao();
	}


	@Bean
	VariantDao variants() {
		return new VariantDao();
	}

	@Bean
	CredentialsDao credentials() {
		return new CredentialsDao();
	}

	@Bean
	SystemMessageDao systemMessageDao() {
		return new SystemMessageDao();
	}

	@Bean
	EmailTemplateDao emailTemplateDaoDao() {
		return new EmailTemplateDao();
	}

	@Bean
	EmailQueueDao emailQueueDao() {
		return new EmailQueueDao();
	}

	@Bean
	EmailQueue emailQueue() {
		return new EmailQueue();
	}

	/*
	 * SERVICES
	 */
	@Bean
	ProductService productService() {
		return new ProductService();
	}


	/*
	 * SYSTEM
	 */
	@Bean
	SystemMailSender mailSender() {
		return new SystemMailSender();
	}

	@Bean
	SystemMailSenderConfig getMailConfig() {
		return new SystemMailSenderConfig();
	}

	@Bean
	MarkdownToHtml getMarkdownToHtmlTransformer() {
		return new MarkdownToHtml();
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
	ExtendedJdbcTemplate db() {
		ExtendedJdbcTemplate out = new ExtendedJdbcTemplate(dataSource());
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

	@Bean
	DbSetup dbSetup() {
		ExtendedJdbcTemplate db = db();
		PropertyBasedConfiguration config = config();

		DbSetup setup = new DbSetup(db, config);
		setupDb(setup);
		return setup;
	}

	public static void setupDb(DbSetup setup) {
		setup.setScriptDirProperty("database.script.dir");
		setup.setScriptOrderProperty("database.tables");

		PropertyBasedConfiguration config = setup.getConfig();
		ExtendedJdbcTemplate db = setup.getDb();

		if (!config.getBoolean("create.tables", false)) {
			String script = setup.toCreateTablesScript();
			String path = null;
			if (!script.isEmpty()) {
				try {
					File tmp = File.createTempFile("script", ".sql");
					path = tmp.getAbsolutePath();
					FileUtils.writeStringToFile(tmp, script, "UTF-8", false);

				} catch (Exception e) {
					log.error("Unable to create temporary file with database DDL script.", e);
				}

				log.error("Database is not ready. Please run the following script as root: >>>\n" + script + "\n<<<.\nScript file saved to: " + path);

				System.exit(0);
			}
		} else {
			setup.createTables();
		}

		if (db.count("SELECT COUNT(*) FROM shop WHERE id = 0") == 0L) {
			db.insert("INSERT INTO shop (id, shop_name, cipher) VALUES (?, ?, ?)", "0", "SYSTEM", new String(Utils.generateRandomCipherKey()));
			db.update("UPDATE shop SET id = 0"); //Yay to MySQL
		}

		if (db.count("SELECT COUNT(*) FROM shop WHERE id = 0") == 0L) {
			throw new IllegalStateException("System shop must have ID = 0.");
		}

	}

}
