package com.univocity.shopify.utils;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;
import com.univocity.shopify.controllers.*;
import com.univocity.shopify.dao.*;
import com.univocity.shopify.email.*;
import com.univocity.shopify.exception.*;
import com.univocity.shopify.model.db.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.*;
import org.springframework.security.web.util.*;
import org.springframework.util.*;
import org.springframework.web.client.*;

import javax.annotation.*;
import javax.servlet.http.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import static com.fasterxml.jackson.databind.DeserializationFeature.*;
import static com.univocity.shopify.utils.Utils.*;

public class App {

	private static final Logger log = LoggerFactory.getLogger(App.class);
	private static final long MAXIMUM_SHOPIFY_CREDITS = 40;

	private static String hostname;

	private Boolean isMySql;
	private String proxy;

	private static final ObjectMapper mapper = new ObjectMapper();
	private static final ConcurrentHashMap<Class, ObjectReader> objectReaders = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<Class, ObjectWriter> objectWriters = new ConcurrentHashMap<>();

	static {
		mapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
	}

	private static final RateLimiter rateLimiter = new RateLimiter(500L);

	@Autowired
	PropertyBasedConfiguration config;

	@Autowired
	private SystemMailSender systemMailSender;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private CredentialsDao credentials;

	@Autowired
	ShopDao shops;

	@Autowired
	SystemMessageDao systemMessageDao;

	@PostConstruct
	public void initialize() {
		testingLocally = config.getBoolean("local.testing", false);
		proxy = config.getProperty("shopify.proxy", "apps/cardano");
	}

	private boolean testingLocally;

	public final boolean isLive() {
		return !testingLocally;
	}

	public final boolean isTestingLocally() {
		return testingLocally;
	}

	public String getRequiredProperty(String property) {
		String value = config.getProperty(property, "");
		if (StringUtils.isBlank(value) && isLive()) {
			log.error("property '{}' is blank, check application properties. Stopping server", property);
			System.exit(0);
		}
		return value;
	}

	public static String getServerName() {
		if (hostname == null) {
			hostname = "localhost";
			try {
				hostname = InetAddress.getLocalHost().getHostName();
			} catch (Exception e) {
				try {
					hostname = InetAddress.getLocalHost().getHostAddress();
				} catch (Exception ex) {
					//ignore
				}
			}
		}
		return hostname;
	}

	public boolean isDatabaseMySQL() {
		if (isMySql == null) {
			isMySql = config.getProperty("database.driver").contains("mysql");
		}
		return isMySql;
	}

	public String getShopifyProxy() {
		return proxy;
	}


	public final RuntimeException notifyError(String msg, Object... args) {
		return notifyError(true, msg, args);
	}

	public final void notifyErrorSuppressException(String msg, Object... args) {
		notifyError(false, msg, args);
	}

	public final RuntimeException notifyError(Throwable e, String msg, Object... args) {
		return notifyError(true, e, msg, args);
	}

	public final void notifyErrorSuppressException(Throwable e, String msg, Object... args) {
		notifyError(false, e, msg, args);
	}

	private final RuntimeException notifyError(boolean throwError, String msg, Object... args) {
		return notifyError(throwError, null, msg, args);
	}

	public final void notify(String title, String msg, Object... args) {
		String message = newMessage(msg, args);
		systemMailSender.sendEmail(title, message);
	}

	private final RuntimeException notifyError(boolean throwError, Throwable e, String msg, Object... args) {
		String errorMessage = newMessage(msg, args);
		log.error(errorMessage, e);
		systemMailSender.sendErrorEmail("Error from " + getClass().getName(), errorMessage, e);
		if (throwError) {
			if (e != null) {
				if (e instanceof RuntimeException) {
					if (errorMessage.equals(e.getMessage())) {
						throw (RuntimeException) e;
					}
				}
				throw new IllegalStateException(errorMessage, e);
			} else {
				throw new IllegalStateException(errorMessage);
			}
		}
		return null;
	}

	public <T, O> T postFor(Class<T> returnType, O object, String shopName, String endpoint) {
		return exchange(returnType, object, shopName, endpoint, HttpMethod.POST, null);
	}

	public <T> T postFor(Class<T> returnType, String url, String[] keyValuePairs) {
		return exchange(returnType, null, url, HttpMethod.POST, keyValuePairs);
	}

	public <T> T postFor(Class<T> returnType, String shopName, String endpoint, String[] keyValuePairs) {
		return exchange(returnType, shopName, endpoint, HttpMethod.POST, keyValuePairs);
	}

	public <T> T getFor(Class<T> returnType, String shopName, String endpoint) {
		return exchange(returnType, shopName, endpoint, HttpMethod.GET, null);
	}

	public void delete(String shopName, String endpoint) {
		exchange(Void.class, shopName, endpoint, HttpMethod.DELETE, null);
	}

	public <T, O> T putFor(Class<T> returnType, O object, String shopName, String endpoint) {
		return exchange(returnType, object, shopName, endpoint, HttpMethod.PUT, null);
	}

	private <T> T exchange(Class<T> returnType, String shopName, String endpoint, HttpMethod method, String[] keyValuePairs) {
		return exchange(returnType, null, shopName, endpoint, method, keyValuePairs);
	}

	private <T> T exchange(Class<T> returnType, Object object, String shopName, String endpoint, HttpMethod method, String[] keyValuePairs) {
		String url;
		HttpHeaders headers = toHeaders(keyValuePairs);
		if (shopName == null) {
			url = endpoint;
		} else {
			shopName = toValidShopName(shopName);
			url = "https://" + shopName + endpoint;
			headers.add("Authorization", credentials.getAuthorizationHeader(shopName));
		}

		HttpEntity entity;
		if (object != null) {
			entity = new HttpEntity(object, headers);
		} else {
			if (method == HttpMethod.POST) {
				headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

				MultiValueMap<String, String> formValues = new LinkedMultiValueMap<>();
				fillMap(formValues, keyValuePairs);

				entity = new HttpEntity<>(formValues, headers);
			} else {
				entity = new HttpEntity(headers);
			}
		}

		long waiting = rateLimiter.waitAndGo();

		ResponseEntity<T> response = restTemplate.exchange(url, method, entity, returnType);

		if (waiting > 1) {
			List<String> callLimitHeader = null;
			try {
				callLimitHeader = response.getHeaders().get("HTTP_X_SHOPIFY_SHOP_API_CALL_LIMIT");
				if (CollectionUtils.isNotEmpty(callLimitHeader)) {
					String value = callLimitHeader.get(0);
					if (StringUtils.isNotBlank(value)) {
						String callLimitValue = StringUtils.substringBefore(value, "/");
						if (StringUtils.isNotBlank(callLimitValue)) {
							long createdCalls = Long.parseLong(callLimitValue);
							long availableCredits = MAXIMUM_SHOPIFY_CREDITS - createdCalls;

							if (availableCredits > 30) {
								rateLimiter.decreaseWaitTime(400);
							} else if (availableCredits > 20) {
								rateLimiter.decreaseWaitTime(300);
							} else if (availableCredits > 10) {
								rateLimiter.decreaseWaitTime(200);
							} else if (availableCredits > 5) {
								rateLimiter.decreaseWaitTime(100);
							}
						}
					}
				}
			} catch (Exception e) {
				log.error("Error processing Shopify call limit header: " + callLimitHeader, e);
			}
		} else { //allow for shopify credits to gradually come back
			rateLimiter.increaseWaitTime(100);
		}

		return response.getBody();
	}

	public static String toValidShopName(String shopName) {
		if (!shopName.contains(".")) {
			shopName += ".myshopify.com";
		}
		return shopName;
	}

	private static HttpHeaders toHeaders(String[] headerPairs) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		fillMap(headers, headerPairs);
		return headers;
	}

	private static void fillMap(MultiValueMap<String, String> map, String[] keyValuePairs) {
		if (keyValuePairs == null) {
			return;
		}
		for (int i = 0; i < keyValuePairs.length; i += 2) {
			String key = keyValuePairs[i];
			String value = keyValuePairs[i + 1];
			map.add(key, value);
		}
	}

	public String getEndpoint(String shopName, String endpoint) {
		String out = "https://" + shopName + "/" + getShopifyProxy() + endpoint;
		return out;
	}

	public void sendEmailToShopOwner(String shopName, MessageType messageType) {
		this.sendEmailToShopOwner(shopName, messageType, null);
	}

	public void sendEmailToShopOwner(String shopName, MessageType messageType, Consumer<SystemMessageRenderer> templateValueSetter) {
		try {
			Shop shop;
			try {
				if (messageType == MessageType.APP_UNINSTALLED) {
					shop = shops.getShopIncludingDeleted(shopName);
				} else {
					shop = shops.getShop(shopName);
				}
			} catch (Exception e) {
				shop = shops.getShopIncludingDeleted(shopName);
			}
			if (shop.getOwnerEmail() != null) {
				SystemMessage message = systemMessageDao.getMessageTemplate(messageType);
				if (message != null) {
					SystemMessageRenderer renderer = message.renderer();
					renderer.setName(shop.getOwnerNameForEmailing());
					renderer.setStoreName(shop);

					if (templateValueSetter != null) {
						templateValueSetter.accept(renderer);
					}

					systemMailSender.sendEmail(new String[]{shop.getOwnerEmail()}, renderer.renderTitle(), renderer.renderBody());
				}
			}
		} catch (Exception e) {
			log.error("Error sending e-mail of type " + messageType + " to owner of shop " + shopName, e);
		}
	}

	public <T> T parseJson(Class<T> outputType, String json) {
		return toObject(outputType, json);
	}

	public <T> T toObject(Class<T> outputType, String json) {
		try {
			T object = (T) readerFor(outputType).readValue(json);
			return object;
		} catch (Exception e) {
			String msg = "Error deserializing object of type '" + outputType + "' from JSON string: " + json;
			log.error(msg, e);
			if (systemMailSender != null) {
				systemMailSender.sendErrorEmail("Cardano Shopify App server error", msg, e);
			}
			return null;
		}
	}

	public static <T> T toObject(Class<T> outputType, HttpServletRequest request) {
		try {
			T object = (T) readerFor(outputType).readValue(request.getReader());
			return object;
		} catch (Exception e) {
			String msg = "Error deserializing object of type '" + outputType + "' from request";
			log.error(msg, e);
			return null;
		}
	}

	private static ObjectReader readerFor(Class outputType) {
		ObjectReader out = objectReaders.get(outputType);
		if (out == null) {
			out = mapper.readerFor(outputType);
			objectReaders.put(outputType, out);
		}
		return out;
	}

	public static ObjectWriter writerFor(Class outputType) {
		ObjectWriter out = objectWriters.get(outputType);
		if (out == null) {
			out = mapper.writerFor(outputType);
		}
		return out;
	}

	public static String toJsonString(Object o) {
		try {
			return writerFor(o.getClass()).writeValueAsString(o);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}


	public boolean isShopInactive(String shopName) {
		return !isShopActive(shopName);
	}

	public boolean isShopActive(String shopName) {
		try {
			return shops.getShop(shopName).isActive();
		} catch (Exception e) {
			notifyError(e, "Error finding shop with name " + shopName);
			return false;
		}
	}

	public Map<String, String[]> getShopifyProxyParams(HttpServletRequest request) {
		Map<String, String[]> params = request.getParameterMap();

		Map<String, String[]> refererParams;
		if (!("/".equals(request.getServletPath()) || "/config".equals(request.getServletPath()))) {
			String referrer = request.getHeader("referer");
			if (StringUtils.isBlank(referrer)) {
				throw new ValidationException("Error processing request");
			}

			referrer = decode(referrer);
			refererParams = getUrlParameters(referrer);
		} else {
			refererParams = new HashMap<>();
		}

		if (isLive()) {
			if (!ShopifyRequestValidation.isHostnameValid(params)) {
				throw new ValidationException("Error processing request");
			}

			String[] shop = params.get("shop");
			if (ArrayUtils.isEmpty(shop) || shop.length > 1 || StringUtils.isBlank(shop[0])) {
				throw new ValidationException("Error processing request");
			}

			if (params.containsKey("signature")) {
				Map<String, String[]> paramsToValidate = getShopifySignatureParameters(params);
				if (!ShopifyRequestValidation.isSignatureValid(paramsToValidate, credentials.sharedSecret())) {
					if (!ShopifyRequestValidation.isSignatureValid(params, credentials.sharedSecret())) { // file download uses all params for some reason.
						throw new ValidationException("Error processing request");
					}
				}
			} else {
				if (!ShopifyRequestValidation.isHmacValid(params, credentials.sharedSecret())) {
					throw new ValidationException("Error processing request");
				}
			}
		}

		refererParams.putAll(params);

		return refererParams;
	}

	public static Map<String, String[]> getShopifySignatureParameters(Map<String, String[]> requestParams) {
		Map<String, String[]> paramsToValidate = new HashMap<>();
		paramsToValidate.put("shop", requestParams.get("shop"));
		paramsToValidate.put("path_prefix", requestParams.get("path_prefix"));
		paramsToValidate.put("timestamp", requestParams.get("timestamp"));
		paramsToValidate.put("signature", requestParams.get("signature"));
		return paramsToValidate;
	}

	public boolean isRequestInvalid(String shopName, HttpServletRequest request) {
		return !isRequestValid(shopName, request);
	}

	public boolean isRequestValid(String shopName, HttpServletRequest request) {
		if (isLive() && !ShopifyRequestValidation.isRequestValid(shopName, request, credentials.sharedSecret())) {
			return false;
		}
		return true;
	}

	public String getApiKey(){
		return config.getProperty("api.key");
	}
}
