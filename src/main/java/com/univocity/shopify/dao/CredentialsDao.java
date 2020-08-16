package com.univocity.shopify.dao;

import com.univocity.shopify.dao.base.*;
import org.apache.commons.lang3.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;

import java.util.*;

import static com.univocity.shopify.utils.Utils.*;


/**
 * @author uniVocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 */
public class CredentialsDao extends BaseDao {

	private static final Logger log = LoggerFactory.getLogger(CredentialsDao.class);

	private String apiKey;
	private String sharedSecret;

	@Autowired
	ShopDao shops;

	public String apiKey() {
		if (apiKey == null) {
			loadCredentials();
		}
		return apiKey;
	}

	public String sharedSecret() {
		if (sharedSecret == null) {
			loadCredentials();
		}
		return sharedSecret;
	}

	private synchronized void loadCredentials() {
		try {
			Map<String, Object> result = db.queryForOptionaLMap("SELECT api_key, shared_secret FROM app WHERE id = " + config.getProperty("app.id"));
			if (result == null) {
				String appId = config.getProperty("app.id");
				String appName = config.getProperty("app.name");
				String apiKey = config.getProperty("api.key");
				String sharedSecret = config.getProperty("shared.secret");

				if (StringUtils.isNotBlank(apiKey) && StringUtils.isNotBlank(sharedSecret)) {
					log.warn("No credentials stored in 'app' table, loading from 'application.properties' and persisting in the database.");
					db.update("INSERT INTO app (id, name, api_key, shared_secret) VALUES (?, ?, ?, ?)", new Object[]{appId, appName, apiKey, sharedSecret});
					log.info("Credentials loaded for API key {} persisted successfully", apiKey);
					this.apiKey = apiKey;
					this.sharedSecret = sharedSecret;
				} else {
					throw new IllegalStateException("No credentials stored in 'app' table nor in the 'application.properties' file.");
				}
			} else {
				apiKey = getMandatoryValue("api_key", result);
				sharedSecret = getMandatoryValue("shared_secret", result);
			}
			log.info("Credentials loaded for API key {} successfully", apiKey);
		} catch (Exception ex) {
			notifyError(ex, "Unable to load cardano app credentials from database. Fatal error.");
			System.exit(1);
		}
	}

	public boolean updateSharedSecret(String newSharedSecret) {
		log.info("Updating shared secret of API key {}", apiKey());
		if (StringUtils.isNotBlank(newSharedSecret)) {
			this.sharedSecret = newSharedSecret;
			try {
				int updateCount = db.update("UPDATE app SET shared_secret = ? WHERE api_key = ?", new Object[]{newSharedSecret, apiKey()});
				if (updateCount == 1) {
					log.info("Shared secret of API key '{}' updated successfully", apiKey());
					shops.invalidateShopTokens();
					return true;
				} else {
					log.info("Shared secret if API key '{}' unchanged.", apiKey());
					return false;
				}
			} catch (Exception ex) {
				notifyError(ex, "Unable to update shared secret.");
			}
		}
		return false;
	}

	public boolean isShopInstalled(String shopName){
		return shops.isShopInstalled(shopName);
	}


	public void registerShopToken(String shopName, String accessToken, boolean refreshing) {
		shops.registerShopToken(shopName, accessToken, refreshing);
	}

	public List<Map<String, Object>> getInvalidatedShopTokens() {
		return shops.getInvalidatedShopTokens();
	}

	public void unregisterShop(String shopName, String json) {
		shops.unregisterShop(shopName, json);
	}

	public String getAuthorizationHeader(String shopName) {
		return shops.getAuthorizationHeader(shopName);
	}
}
