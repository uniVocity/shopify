package com.univocity.shopify.controllers.api;

import com.univocity.parsers.common.input.*;
import com.univocity.shopify.*;
import com.univocity.shopify.controllers.*;
import com.univocity.shopify.dao.*;
import com.univocity.shopify.email.*;
import com.univocity.shopify.model.db.*;
import com.univocity.shopify.model.shopify.*;
import com.univocity.shopify.utils.*;
import org.apache.commons.lang3.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.*;

import javax.servlet.http.*;
import java.util.*;
import java.util.concurrent.*;

import static com.univocity.shopify.utils.Utils.*;


/**
 * @author uniVocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 */
@RestController
@Import(ApplicationConfiguration.class)
public class AuthenticationService {

	private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

	@Autowired
	App app;

	@Autowired
	CredentialsDao credentials;

	@Autowired
	ShopifyApiService apiService;

	@Autowired
	SystemMailSender systemMailSender;

	@Autowired
	ShopDao shops;

	@Autowired
	SystemMessageDao systemMessageDao;

	@Autowired
	ShopifyApiService shopifyApiService;

	private final Object tokenRefreshLock = new Object();

	private String refreshToken;

	private TimedCache<String, Long> nonces = new TimedCache<>(TimeUnit.MINUTES.toMillis(30), 1000);

	private static String buildAccessTokenUrl(String shopName) {
		if (StringUtils.isBlank(shopName)) {
			return null;
		}
		return Shop.getAdminUrl(shopName) + "/oauth/access_token";
	}

	@RequestMapping(value = "/", method = RequestMethod.GET)
	public RedirectView firstAccess(@RequestParam("shop") String shopName, HttpServletRequest request) {
		if(credentials.isShopInstalled(shopName)){
			Shop shop = shops.getShop(shopName);
			if(shop.isActive()){
				log.info("Shop {} hitting /. Already installed and active. Redirecting to app in admin page at '/apps/cardano'", shopName);
				return new RedirectView(Shop.getAdminUrl(shop.getShopName()) + "/apps/cardano");
			} else {
				log.info("Shop {} hitting /. Already installed but not active. Redirecting to '/billing/auth' for installation", shopName);
				return new RedirectView(app.getEndpoint(shop.getShopName(), "/billing/auth"));
			}
		} else {
			log.info("Shop {} hitting /. Not installed. Redirecting to '/install' for installation", shopName);
			return generateAppInstallLink(shopName, "false", request);
		}
	}

	@RequestMapping(value = "/install", method = RequestMethod.GET)
	public RedirectView generateAppInstallLink(@RequestParam("shop") String shopName, HttpServletRequest request) {
		log.info("Received app installation request for shop {}", shopName);
		return generateAppInstallLink(shopName, "false", request);
	}

	public RedirectView generateAppInstallLink(String shopName, String online, HttpServletRequest request) {
		String nonce = UUID.randomUUID().toString().replaceAll("-", "");
		nonces.put(nonce, System.currentTimeMillis());

		String apiKey = credentials.apiKey();
		String redirectEndpoint = "/login";

		String baseUrl = getBaseUrl(request);
		if (baseUrl.endsWith(":80")) {
			baseUrl = baseUrl.substring(0, baseUrl.length() - 3);
		} else if (baseUrl.endsWith(":443")) {
			baseUrl = baseUrl.substring(0, baseUrl.length() - 4);
		}

		boolean isOnline = Boolean.parseBoolean(online);
		String option = isOnline ? "per-user" : "";

		String url = Shop.getAdminUrl(shopName) + "/oauth/authorize?client_id=" + apiKey + "&scope=read_orders,read_customers,read_products&redirect_uri=" + baseUrl + redirectEndpoint + "&state=" + nonce + "&grant_options[]=" + option;
		log.info("Redirecting to installation URL of shop {}: {}", shopName, url);

		RedirectView redirectView = new RedirectView();
		redirectView.setUrl(url);

		return redirectView;
	}


	@RequestMapping("/login")
	//Redirection URL (required). //Shopify calls this: https://example.org/some/redirect/uri?code={authorization_code}&hmac=da9d83c171400a41f8db91a950508985&timestamp=1409617544&state={nonce}&shop={hostname}
	public RedirectView auth(@RequestParam("code") String authorizationCode, @RequestParam("shop") String hostname, @RequestParam(value = "state", required = false) String state, HttpServletRequest request) {
		log.info("Processing shop registration: Shop {}", hostname);
		RedirectView redirectView = new RedirectView();
		if(state == null){
			log.info("Received app reinstall request from shop {}, redirecting to setting page", hostname);
			redirectView.setUrl(Shop.getAdminUrl(hostname) + "/apps/cardano");
			return redirectView;
		}
		redirectView.setUrl(Shop.getAdminUrl(hostname));

		Map<String, String[]> parameters = request.getParameterMap();

		if (!ShopifyRequestValidation.isHostnameValid(hostname)) {
			log.warn("Got invalid app installation request from shop {}. Invalid hostname. Request: {}", hostname, printRequest(request));
			return redirectView;
		}

		//nonce must exist.
		Long nonceTimestamp = nonces.get(state);
		if (nonceTimestamp == null) {
			log.warn("Got invalid app installation request from shop {}. Nonce does not exist for state {}. Request: {}", hostname, state, printRequest(request));
			return redirectView;
		}

		if (!ShopifyRequestValidation.isHmacValid(parameters, credentials.sharedSecret())) {
			log.warn("Got invalid app installation request from shop {}. Invalid HMAC. Request: {}", hostname, state, printRequest(request));
			return redirectView;
		}

		// Shopify expects a POST to https://{shop}/admin/oauth/access_token
		//  Headers:
		//   * The API Key for the app.
		//   * The Shared Secret for the app.
		//   * The authorization code provided in the redirect described above.
		String requestUrl = buildAccessTokenUrl(hostname);
		log.info("Processing initial OAUTH authentication of shop {}", requestUrl);
		Token token = app.postFor(Token.class, requestUrl, new String[]{"client_id", credentials.apiKey(), "client_secret", credentials.sharedSecret(), "code", authorizationCode});

		token.setShopName(hostname);

		credentials.registerShopToken(token.getShopName(), token.accessToken, false);
		request.getSession().setAttribute("token", token.accessToken);

		//remove successfully authenticated nonce.
		nonces.remove(state);

		shops.reloadShop(token.getShopName());

		// X-Shopify-Access-Token: {access_token}
		apiService.registerWebHooks(token.getShopName());

		ShopDetails shopDetails = null;
		try {
			shopDetails = apiService.getShopDetails(token.getShopName(), null);

			shops.updateShopOwnerDetails(shops.getShop(token.getShopName()), shopDetails);
			app.sendEmailToShopOwner(token.getShopName(), MessageType.APP_INSTALLED);
		} catch (Exception e) {
			log.error("Error updating shop owner details. Shop: " + hostname, e);
		}

		app.notify("App installed notification", "Shop " + token.getShopName() + " installed your app.\nDetails: " + shopDetails);

//		Charge charge = shopifyApiService.setupMonthlyFee(token.getShopName(), !utils.isLive(), null);

		redirectView.setUrl(Shop.getAdminUrl(token.getShopName()) + "/apps/cardano");

		return redirectView;
	}

	//Requires going to the server and hitting the "refresh" url with the new secret.
	@RequestMapping("/refresh_tokens")
	public void refreshTokens(@RequestParam("current_secret") String currentSecret, @RequestParam("new_secret") String newSecret, @RequestParam("refresh_token") String refreshToken, HttpServletRequest request) {
		Utils.executeLocal(request, () -> {
			synchronized (tokenRefreshLock) {
				if (StringUtils.equals(refreshToken, this.refreshToken) || StringUtils.equals(newSecret, credentials.sharedSecret()) || !StringUtils.equals(currentSecret, credentials.sharedSecret())) {
					log.warn("Received invalid refresh token request: {} ", Utils.printRequest(request));
					return;
				}
				log.info("Processing token refresh request.");
				this.refreshToken = refreshToken;
				if (credentials.updateSharedSecret(newSecret)) {
					final List<Map<String, Object>> toRefresh = credentials.getInvalidatedShopTokens();
					if (toRefresh.isEmpty()) {
						log.info("No tokens to refresh");
					} else {
						log.info("Updating tokens of {} elements", toRefresh.size());
						//runs in a new thread as this may take a while
						new Thread() {
							public void run() {
								setName("Shop token refresh thread");
								log.info("Updating tokens of {} elements", toRefresh.size());
								ElasticCharAppender errors = borrowBuilder();
								try {
									int errorCount = 0;
									for (Map<String, Object> e : toRefresh) {
										if (!refreshToken(e, newSecret)) {
											String msg = "Token refresh failed for shop " + e.get("shop_name");
											errors.append(msg);
											errors.append('\n');
											log.error(msg);
											errorCount++;
										}
									}
									if (errors.length() > 0) {
										log.error("Token refresh process finished with {} errors and {} tokens successfully refreshed", errorCount, toRefresh.size());
										systemMailSender.sendErrorEmail("Token refresh process errors", errors.toString(), null);
									} else {
										log.info("Token refresh process finished with {} tokens successfully refreshed", toRefresh.size());
									}
								} finally {
									releaseBuilder(errors);
								}
							}
						}.start();
					}
				}
			}
		});
	}

	public boolean refreshToken(Map<String, Object> entry, String newSecret) {
		String shopName = String.valueOf(entry.get("shop_name"));
		String oldToken = String.valueOf(entry.get("shop_token"));
		String requestUrl = buildAccessTokenUrl(shopName);

		try {
			Token newToken = app.postFor(Token.class, requestUrl, new String[]{"client_id", credentials.apiKey(), "client_secret", newSecret, "refresh_token", refreshToken, "access_token", oldToken});
			credentials.registerShopToken(shopName, newToken.accessToken, true);
			return true;
		} catch (Exception ex) {
			log.error("Could not refresh old token for shop '" + shopName + "'", ex);
		}
		return false;
	}


	public boolean isRequestValid(String requestBody, HttpServletRequest request) {
		String shop = Utils.getShopName(request);
		if (shop != null) {
			if (!ShopifyRequestValidation.isHostnameValid(shop)) {
				return false;
			}

			String hmac = request.getHeader("X-Shopify-Hmac-Sha256");
			if (hmac != null) {
				return ShopifyRequestValidation.isHmacValid(requestBody, request, credentials.sharedSecret());
			}
		}
		return false;
	}
}
