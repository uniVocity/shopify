

package com.univocity.shopify.controllers;

import com.univocity.parsers.common.input.*;
import com.univocity.shopify.utils.*;
import org.apache.commons.lang3.*;
import org.slf4j.*;

import javax.crypto.*;
import javax.crypto.spec.*;
import javax.servlet.http.*;
import java.nio.charset.*;
import java.security.*;
import java.util.*;

import static com.univocity.shopify.utils.Utils.*;
import static org.apache.commons.lang3.ArrayUtils.*;


/**
 * @author uniVocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 */
public class ShopifyRequestValidation {

	private static final Logger log = LoggerFactory.getLogger(ShopifyRequestValidation.class);

	private static final Charset ENCODING = StandardCharsets.UTF_8;
	private static final String ALGORITHM = "HmacSHA256";

	private static Mac lastMac;
	private static String lastSecret;

	public static boolean isHmacValid(String requestBody, HttpServletRequest request, String secret) {
		String hmac = request.getHeader("X-Shopify-Hmac-Sha256");
		if (StringUtils.isBlank(hmac)) {
			log.warn("Null/Blank HMAC in response to request to '{}'", Utils.printRequest(request));
			return false;
		}

		try {
			byte[] requestBodyBytes = requestBody.getBytes("UTF-8");
			byte[] macData = calculateHmac(requestBodyBytes, secret);
			String result = Base64.getEncoder().encodeToString(macData);
			return result.equals(hmac);
		} catch (Exception ex) {
			log.error("Could not read body of request: " + Utils.printRequest(request), ex);
			return false;
		}
	}

	public static boolean isRequestValid(String shopName, HttpServletRequest request, String secret) {
		return isHostnameValid(shopName) && ShopifyRequestValidation.isHmacValid(request.getParameterMap(), secret);
	}

	public static boolean isHmacValid(Map<String, String[]> requestParams, String secret) {
		return isHmacValid(requestParams, secret, "&");
	}

	public static boolean isSignatureValid(Map<String, String[]> requestParams, String secret) {
		return isHmacValid(requestParams, secret, "");
	}

	/**
	 * Validates HMAC according to specification provided by Shopify (https://help.shopify.com/api/getting-started/authentication/oauth)
	 *
	 * @param requestParams the request parameters whose values will be used to perform shopify's HMAC validation
	 * @param secret        the cardano app secret key
	 *
	 * @return {@code true} if the HMAC is valid, {@false} if it's not valid or if there was an exception.
	 */
	private static boolean isHmacValid(Map<String, String[]> requestParams, String secret, String separator) {
		HashMap<String, String[]> parameters = new HashMap<>(requestParams);
		String hmac = removeParameter(parameters, "hmac");
		if (hmac == null) {
			hmac = removeParameter(parameters, "signature");
			separator = "";
			if (hmac == null) {
				return false;
			}
		} else {
			removeParameter(parameters, "signature");
		}

		TreeMap<String, String> sorted = new TreeMap<>();
		parameters.keySet().forEach(key -> sorted.put(key, getParameter(parameters, key)));

		String sep = separator;

		byte[] messageBytes;
		ElasticCharAppender message = borrowBuilder();
		try {
			sorted.entrySet().forEach(entry -> {
				message.append(entry.getKey());
				message.append('=');
				message.append(entry.getValue());
				message.append(sep);
			});
			if (separator.length() == 1) {
				if (message.length() > 1) {
					message.ignore(1);
				}
			}

			messageBytes = message.toString().getBytes(ENCODING);
		} finally {
			releaseBuilder(message);
		}
		byte[] macData = calculateHmac(messageBytes, secret);

		String result = String.format("%064x", new java.math.BigInteger(1, macData));

		return result.equals(hmac);
	}

	static byte[] calculateHmac(byte[] messageBytes, String secret) {
		Mac mac;
		if (!secret.equals(lastSecret)) {
			byte[] secretBytes = secret.getBytes(ENCODING);

			try {
				mac = Mac.getInstance(ALGORITHM);
			} catch (NoSuchAlgorithmException e) {
				log.error("Can't validate HMAC. Unable initialize algorithm " + ALGORITHM, e);
				return EMPTY_BYTE_ARRAY;
			}

			final SecretKeySpec secretKey = new SecretKeySpec(secretBytes, ALGORITHM);
			try {
				mac.init(secretKey);
			} catch (InvalidKeyException e) {
				log.error("Can't validate HMAC. Invalid secret key for algorithm " + ALGORITHM, e);
				return EMPTY_BYTE_ARRAY;
			}

			lastMac = mac;
			lastSecret = secret;
		} else {
			mac = lastMac;
		}

		byte[] macData = mac.doFinal(messageBytes);

		return macData;
	}

	public static boolean isHostnameValid(Map<String, String[]> params) {
		String[] value = params.get("shop");
		if (ArrayUtils.isEmpty(value)) {
			return false;
		}
		return isHostnameValid(value[0]);
	}

	/**
	 * Validates shopify hostname parameter (must end with .shopify.com and contains only a-z, 0-9, dot or hyphen).
	 *
	 * @param shop the hostname
	 *
	 * @return {@code true} if it's a valid host.
	 */
	public static boolean isHostnameValid(String shop) {
		if (shop == null) {
			return false;
		}
		for (int i = 0; i < shop.length(); i++) {
			char ch = shop.charAt(i);
			if (ch >= 'a' && ch <= 'z') {
				continue;
			}
			if (ch >= '0' && ch <= '9') {
				continue;
			}
			if (ch == '.' || ch == '-') {
				continue;
			}
			return false;
		}
		return shop.endsWith("myshopify.com");
	}
}
