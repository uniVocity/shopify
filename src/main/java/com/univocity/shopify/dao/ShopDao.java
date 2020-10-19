package com.univocity.shopify.dao;


import com.univocity.shopify.controllers.api.*;
import com.univocity.shopify.dao.base.*;
import com.univocity.shopify.email.*;
import com.univocity.shopify.exception.*;
import com.univocity.shopify.model.db.*;
import com.univocity.shopify.model.shopify.*;
import com.univocity.shopify.utils.database.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.jdbc.core.*;

import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;

import static com.univocity.shopify.utils.Utils.*;
import static org.apache.commons.lang3.StringUtils.*;

/**
 * @author uniVocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 */
public class ShopDao extends BaseDao {

	private static final Logger log = LoggerFactory.getLogger(ShopDao.class);

	@Autowired
	AuthenticationService authenticationService;

	@Autowired
	CredentialsDao credentials;

	@Autowired
	SystemMailSender systemMailSender;

	@Autowired
	ExtendedJdbcTemplate db;

	private Map<String, Shop> shopsByName;
	private Map<Long, Shop> shopsById;

	public void clearCaches() {
		shopsByName.clear();
		shopsById.clear();
	}

	private static final RowMapper<Shop> shopRowMapper = (rs, rowNum) -> {
		Shop out = new Shop();
		out.mapRow(rs, rowNum);
		return out;
	};

	private Map<String, Shop> getShopsByName() {
		if (shopsByName == null) {
			synchronized (this) {
				if (shopsByName == null) {
					loadShops();
				}
			}
		}
		return shopsByName;
	}

	private Map<Long, Shop> getShopsById() {
		if (shopsById == null) {
			synchronized (this) {
				if (shopsById == null) {
					loadShops();
				}
			}
		}
		return shopsById;
	}

	private Map<?, Shop> getMap(boolean shopsById) {
		if (shopsById) {
			return getShopsById();
		} else {
			return getShopsByName();
		}
	}

	private Shop query(boolean shopsById, Object valueToMatch, String columnToMatch, String columnToGet) {
		Shop shop = getMap(shopsById).get(valueToMatch);
		if (shop == null) {
			Object result = db.queryForOptionalObject("SELECT " + columnToGet + " FROM shop WHERE " + columnToMatch + " = ? AND deleted_at IS NULL AND id > 0", new Object[]{valueToMatch}, Object.class);
			if (result != null) {
				loadShops();
				shop = getMap(shopsById).get(valueToMatch);
			}
		}

		if (shop == null) {
			String msg = "Unknown shop (" + columnToMatch + " = '" + valueToMatch + "' not found), cannot process request.";
			ValidationException error = new ValidationException(msg);
			log.error(msg, error);
			throw error;
		}

		return shop;
	}

	public String getShopName(Long shopId) {
		return getShop(shopId).getShopName();
	}

	public Long getShopId(String shopName) {
		return getShop(shopName).getId();
	}

	public Shop getShop(Long shopId) {
		return query(true, shopId, "id", "shop_name");
	}

	public Shop getShopByDomain(String domain) {
		return db.queryForOptionalObject("SELECT * FROM shop WHERE domain = ? AND id > 0", shopRowMapper, domain);
	}

	public Shop getShop(String shopName) {
		return query(false, shopName.toLowerCase(), "shop_name", "id");
	}

	public Long getShopIdIncludingDeleted(String shopName) {
		Number id = db.queryForOptionalObject("SELECT id FROM shop WHERE shop_name = ?", new Object[]{shopName}, Number.class);
		if (id == null) {
			return null;
		}
		return id.longValue();
	}

	public Shop getShopByShopifyIdIncludingDeleted(Long shopifyId) {
		return db.queryForOptionalObject("SELECT * FROM shop WHERE shopify_id = ?", shopRowMapper::mapRow, shopifyId);
	}

	public Shop getShopIncludingDeleted(String shopName) {
		shopName = shopName.toLowerCase();
		return returnCachedInstanceIfAvailable(db.queryForOptionalObject("SELECT * FROM shop WHERE shop_name = ?", shopRowMapper::mapRow, shopName));
	}

	public Shop getShopIncludingDeleted(Long shopId) {
		return returnCachedInstanceIfAvailable(db.queryForOptionalObject("SELECT * FROM shop WHERE id = ?", shopRowMapper::mapRow, shopId));
	}

	public Shop getShopIfExists(String shopName) {
		shopName = shopName.toLowerCase();
		return returnCachedInstanceIfAvailable(db.queryForOptionalObject("SELECT * FROM shop WHERE deleted_at IS NULL AND shop_name = ?", shopRowMapper::mapRow, shopName));
	}

	public Shop getShopIfExists(Long shopId) {
		return returnCachedInstanceIfAvailable(db.queryForOptionalObject("SELECT * FROM shop WHERE deleted_at IS NULL AND id = ?", shopRowMapper::mapRow, shopId));
	}

	private Shop returnCachedInstanceIfAvailable(Shop shop) {
		if (shop == null) {
			return null;
		}
		Shop out = getShopsById().get(shop.getId());
		if (out == null) {
			return shop;
		}
		return out;
	}

	public Collection<Shop> getShops() {
		return Collections.unmodifiableCollection(getShopsById().values());
	}

	private synchronized void loadShops() {
		List<Shop> shops = db.query("SELECT * FROM shop WHERE deleted_at IS NULL AND id > 0", shopRowMapper::mapRow);

		Map<String, Shop> byName = new ConcurrentHashMap<>();
		Map<Long, Shop> byId = new ConcurrentHashMap<>();

		shops.forEach(shop -> addShopToIndex(shop, byName, byId));

		this.shopsById = byId;
		this.shopsByName = byName;
	}

	public void reloadShop(String shopName) {
		log.info("Reloading shop {}", shopName);
		Shop shop = shops.getShop(shopName);
		if (shop != null) {
			addShopToIndex(shop.getShopName(), shop.getId());
		} else {
			log.info("Shop {} not cached", shopName);
		}
	}

	String getToken(String shopName) {
		return getShop(shopName).getShopToken();
	}

	public boolean isShopInstalled(String shopName) {
		return db.count("SELECT COUNT(*) FROM shop WHERE shop_name = ? AND shop_token IS NOT NULL AND deleted_at IS NULL", shopName) == 1;
	}

	public void registerShopToken(String shopName, String shopToken, boolean refreshing) {
		shopName = shopName.toLowerCase();
		Shop shop = getShopsByName().get(shopName);

		if (refreshing && shop != null && shop.getShopToken() != null) {
			try {
				int updateCount = updateShopToken(shopName, shopToken);
				if (updateCount == 1) {
					log.info("Updated token of shop '{}'", shopName);
				} else {
					log.warn("Error updating token of shop '{}'. No rows affected. Retrying with insert operation.", shopName);
				}
			} catch (Exception ex) {
				String msg = "Error updating token of shop '" + shopName + "'";
				log.error(msg, ex);
				systemMailSender.sendErrorEmail("Cardano app server error", msg, ex);
				printFailedShopAndToken(shopName, shopToken);
			}
		} else {
			insertShopToken(shopName, shopToken);
		}
		getShop(shopName).setShopToken(shopToken);
	}

	private void printFailedShopAndToken(String shopName, String shopToken) {
		String msg = ">> SHOP '" + shopName + "' =TOKEN=> [" + shopToken + "]";
		System.err.println(msg);
		System.out.println(msg);
		log.warn(msg);
	}

	private int updateShopToken(String shopName, String shopToken) {
		return db.update("UPDATE shop SET shop_token = ?, invalid = 0, deleted_at = NULL WHERE shop_name = ?", new Object[]{shopToken, shopName});
	}

	public void registerShop(String shopName) {
		insertShopToken(shopName.toLowerCase(), null);
	}

	private void addShopToIndex(Shop shop) {
		addShopToIndex(shop, getShopsByName(), getShopsById());
	}

	private void addShopToIndex(Shop shop, Map<String, Shop> indexByName, Map<Long, Shop> indexById) {
		if (shop.getId() != 0) {
			indexByName.put(shop.getShopName(), shop);
			indexById.put(shop.getId(), shop);
		}
	}

	private void addShopToIndex(String shopName, Number id) {
		Shop shop = db.queryForOptionalObject("SELECT * FROM shop WHERE shop_name = ? AND deleted_at IS NULL AND id > 0", shopRowMapper, shopName);
		if (shop == null) {
			log.info("Removing shop from cache: {}. ID: {}", shopName, id);
			getShopsById().remove(id);
			getShopsByName().remove(shopName);
		} else {
			addShopToIndex(shop, getShopsByName(), getShopsById());
			log.info("Registered new token for shop '{}' (ID: {})", shopName, id);
		}
	}

	private void insertShopToken(String shopName, String shopToken) {
		try {
			boolean cached = getShopsByName().containsKey(shopName.toLowerCase());

			if (!cached) {
				Number id = db.queryForOptionalObject("SELECT id FROM shop WHERE shop_name = ? AND deleted_at IS NOT NULL AND id > 0", new Object[]{shopName}, Number.class);
				if (id != null) { //reinstalled app.
					updateShopToken(shopName, shopToken);
				} else {
					id = db.insertReturningKey("INSERT INTO shop (shop_token, shop_name, cipher) VALUES (?, ?, ?)", "id", new Object[]{shopToken, shopName,
							new String(generateRandomCipherKey())});
				}
				if (id != null) {
					addShopToIndex(shopName, id);
				} else {
					log.warn("Registered new token for shop '{}' but failed to obtain its ID", shopName);
				}
			} else {
				updateShopToken(shopName, shopToken);
			}
		} catch (Exception ex) {
			log.warn("Error registering token of shop '" + shopName + "'. Retrying with update operation", ex);
			try {
				if (updateShopToken(shopName, shopToken) == 0) {
					throw ex;
				}
			} catch (Exception e) {
				notifyError(e, "Unable to register token of shop '" + shopName + "', retry operation failed");
			}
		}
	}

	List<Map<String, Object>> getInvalidatedShopTokens() {
		return db.queryForList("SELECT shop_name, shop_token FROM shop WHERE invalid = 1 AND deleted_at IS NULL AND id > 0");
	}

	void invalidateShopTokens() {
		try {
			synchronized (this) {
				int updateCount = db.update("UPDATE shop SET invalid = 1 WHERE deleted_at IS NULL AND id > 0");
				getShopsByName().values().forEach(shop -> shop.setShopToken(null));
				log.info("Invalidated {} shop tokens", updateCount);
			}
		} catch (Exception ex) {
			String msg = "Unable to invalidate shop tokens";
			log.error(msg, ex);
			systemMailSender.sendErrorEmail("Cardano server error", msg, ex);
		}
	}

	void unregisterShop(String shopName, String details) {
		try {
			if (db.update("UPDATE shop SET deleted_at = ?, uninstall_info = ?, active = 0, shop_token = NULL WHERE shop_name = ? AND deleted_at IS NULL AND id > 0", new Object[]{new Date(), details, shopName}) == 1) {
				log.info("Deleted token of shop '{}'. Shop unregistered successfully", shopName);
			} else {
				log.debug("Shop '{}' already unregistered", shopName);
			}
			Shop shop = getShopsByName().remove(shopName.toLowerCase());
			if (shop != null) {
				shop.setActive(false); //just in case anything has a reference to it.
				shop.setShopToken(null);
				getShopsById().remove(shop.getId());
			}
		} catch (Exception ex) {
			String msg = "Error unregistering shop '" + shopName + "'";
			log.error(msg, ex);
			systemMailSender.sendErrorEmail("Cardano app server error", msg, ex);
		}
	}

	String getAuthorizationHeader(String shopName) {
		Shop shop = getShop(shopName);
		String auth = shop.getShopAuth();
		if (auth == null) {
			String apiKey = credentials.apiKey();
			String password = getToken(shopName);
			String credentials = apiKey + ":" + password;
			auth = Base64.encodeBase64String(credentials.getBytes(StandardCharsets.UTF_8));
			auth = "Basic " + auth;
			shop.setShopAuth(auth);
		}
		return auth;
	}

	public void updateShop(Shop shop, String[] fields, Object[] values) {
		notNull(shop, "Shop");
		String upd = db.createUpdateStatement("shop", fields) + " WHERE shop_name = ? AND id = ? AND deleted_at IS NULL AND id > 0";

		if (db.update(upd, ArrayUtils.addAll(values, shop.getShopName(), shop.getId())) == 1) {
			addShopToIndex(shop);
			log.info("Shop '{}' updated successfully", shop.getId());
		} else {
			log.debug("Shop '{}' not changed", shop.getId());
		}
	}


	public void persist(Shop shop) {
		LinkedHashSet<String> ids = new LinkedHashSet<>();
		ids.add("id");
		Map<String, Object> values = shop.toMap();
		db.update("shop", values, ids);
	}

	public void updateShopOwnerDetails(Shop shop, ShopDetails shopDetails) {
		if (shopDetails == null || shop == null) {
			return;
		}

		shop.setCustomerEmail(shopDetails.customerEmail);
		shop.setDomain(shopDetails.domain);
		shop.setPhone(shopDetails.phone);
		shop.setShopOwner(shopDetails.shopOwner);
		shop.setTimezone(shopDetails.timezone);
		shop.setCountry(shopDetails.country);
		shop.setCity(shopDetails.city);
		shop.setOwnerEmail(shopDetails.email);
		shop.setShopifyId(shopDetails.id);

		String mailList = shop.getNotificationEmailList();
		if (isBlank(mailList) || !mailList.contains(shopDetails.email)) {
			if (isBlank(mailList)) {
				mailList = shopDetails.email;
			} else {
				mailList += ';' + shopDetails.email;
			}
			shop.setNotificationEmailList(mailList);
		}

		if (db.update("" +
				"UPDATE shop SET updated_at = ?, shopify_id = ?, " +
				"owner_email = ?, owner_name = ?, customer_email = ?, domain = ?, phone = ?, timezone = ?, country = ?, city = ?, email_notification_list = ? " +
				"WHERE shop_name = ? AND id = ? AND deleted_at IS NULL AND id > 0", new Object[]{
				new Date(),

				shop.getShopifyId(),
				shop.getOwnerEmail(),
				shop.getShopOwnerName(),
				shop.getCustomerEmail(),
				shop.getDomain(),
				shop.getPhone(),
				shop.getTimezone(),
				shop.getCountry(),
				shop.getCity(),

				shop.getNotificationEmailList(),
				shop.getShopName(),
				shop.getId()
		}) == 1) {
			addShopToIndex(shop);
			log.info("Shop '{}' updated successfully with owner details: {}", shop.getShopName(), shopDetails);
		} else {
			log.debug("Shop '{}' not updated with owner details: {}", shop.getShopName(), shopDetails);
		}
	}

	public void updateShopPreferences(Shop shop) {
		Map<String, Object> data = shop.toMap();
		int rowsAffected = db.update("shop", data, new LinkedHashSet<>(Arrays.asList("id", "shopify_id", "shop_name")), "deleted_at IS NULL AND id > 0");

		if (rowsAffected == 1) {
			addShopToIndex(shop);
			log.info("Shop '{}' updated successfully", shop.getShopName());
		} else {
			throw new ApplicationStateException("Could not update shop " + shop);
		}
	}

	public void deactivateShop(Shop shop) {
		try {
			db.update("UPDATE shop SET active = 0, webhooks = NULL WHERE id = " + shop.getId());
			shop.setActive(false);
			shop.setWebhooks(null);
		} finally {
			systemMailSender.sendEmail("Disabling shop " + shop.getId(), "Please review");
		}
	}

	public void evict(String shopName) {
		shopName = shopName.toLowerCase();
		Shop shop = getShopsByName().remove(shopName);
		if (shop != null) {
			shop.setShopAuth(null);
			getShopsById().remove(shop.getId());
		}
	}

	public void activateShop(Shop shop) {
		if (shop.isActive()) {
			return;
		}

		shop.setActive(true);
		db.update("UPDATE shop SET active = 1 WHERE id = " + shop.getId());
	}
}