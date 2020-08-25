package com.univocity.shopify.controllers.view;

import com.univocity.parsers.common.input.*;
import com.univocity.shopify.dao.*;
import com.univocity.shopify.exception.*;
import com.univocity.shopify.model.db.*;
import com.univocity.shopify.utils.*;
import org.apache.commons.io.*;
import org.slf4j.*;

import javax.servlet.http.*;
import java.io.*;
import java.math.*;
import java.nio.charset.*;
import java.text.*;
import java.util.*;
import java.util.function.*;

import static com.univocity.shopify.controllers.view.ActivationController.*;
import static com.univocity.shopify.controllers.view.ShopPreferencesFormController.*;
import static com.univocity.shopify.utils.Utils.*;

public class ViewUtils {

	static final Logger log = LoggerFactory.getLogger(ViewUtils.class);
	static final String js = readTextFromResource("template/layout/app.js", StandardCharsets.UTF_8);

	static String getDefaultAmountMessage(BigDecimal current, BigDecimal productAmount, BigDecimal maxAmount, String msg, char symbol) {
		if (current != null) {
			return "";
		}
		if (productAmount == null) {
			return toDefaultLbl("0", symbol);
		}

		if (maxAmount != null) {
			if (maxAmount.compareTo(productAmount) < 0) {
				return toDefaultLbl(maxAmount.stripTrailingZeros().toPlainString(), symbol);
			} else {
				return toDefaultLbl(productAmount.stripTrailingZeros().toPlainString(), symbol);
			}
		}

		return toDefaultLbl(msg, symbol);
	}

	static String getDefaultDaysMessage(Integer current, Integer productDays, Integer shopDays, String msg) {
		if (current != null) {
			return "";
		}
		Integer days = productDays == null ? shopDays : productDays;
		if (days == null || days == 0) {
			return toDefaultLbl(msg, '\0');
		}
		return toDefaultLbl(days + " days", '\0');
	}

	static String toDefaultLbl(String msg, char symbol) {
		String l = symbol == '$' ? "$" : "";
		String r = symbol == '%' ? "%" : "";

		return "<strong>(default: " + l + msg + r + ")</strong>";
	}


	static String modifySettings(App app, String action, HttpServletRequest request, Function<Map<String, String[]>, String> function) {
		Map<String, String[]> params;

		try {
			if (app.isTestingLocally()) {
				params = new HashMap<>();
				params.putAll(request.getParameterMap());
			} else {
				params = app.getShopifyProxyParams(request);
			}
		} catch (ValidationException e) {
			log.error("Error performing '" + action + "' action. Request details: " + printRequest(request), e);
			return Utils.returnErrorJson(e.getMessage());
		}

		if (!params.containsKey("shop")) {
			if (app.isTestingLocally()) {
				String referer = request.getHeader("referer");
				String[] refererShop = getUrlParameters(referer).get("shop");
				if (refererShop == null) {
					return Utils.returnInvalidRequestJson();
				}
				params.put("shop", refererShop);
			} else {
				return Utils.returnInvalidRequestJson();
			}
		}

		String shopName = params.get("shop")[0];

		if (app.isShopInactive(shopName)) {
			if (!(ACTIVATE_APP_ACTION.equals(action) || AUTHORIZE_APP_ACTION.equals(action) || PREFERENCES_VIEW_ACTION.equals(action))) {
				log.warn("Refusing to process '{}' action as shop is inactive", action);
				return Utils.returnErrorJson("Cardano payment system disabled. Please reinstall the app on your shop.");
			}
		}

		String[] ids = params.get("id");
		String shopifyId = null;
		if (ids != null && ids.length > 0) {
			shopifyId = ids[0];
		}

		try {
			log.debug("Processing action: '{}'. shopify id: {}, shop: {}", action, shopifyId, shopName);
			return function.apply(params);
		} catch (ValidationException e) {
			log.error("Validation error performing '" + action + "' action. Request details: " + printRequest(request), e);
			return returnErrorJson(e.getMessage());
		} catch (Exception e) {
			log.error("Error performing '" + action + "' action. Shopify ID: " + shopifyId + ", Shop: " + shopName + ". Request details: " + printRequest(request), e);
			return returnErrorJson("Error processing request");
		}
	}


	static String getFormattedDate(SimpleDateFormat format, java.sql.Date date) {
		if (date == null) {
			return "-";
		}
		return format.format(date);
	}

	static void processReportDownload(App app, String shopName, String action, HttpServletRequest request, HttpServletResponse response, Supplier<File> supplier) {
		if (app.isRequestInvalid(shopName, request)) {
			log.warn("Received invalid " + action + " request: {}", printRequest(request));
			return;
		}

		File file = supplier.get();

		if (file == null || !file.exists()) {
			return;
		}

		response.setContentType("application/octet-stream");
		response.setHeader("Content-Disposition", "filename=\"" + file.getName() + "\"");
		try {
			FileUtils.copyFile(file, response.getOutputStream());
		} catch (Exception e) {
			log.error("Error performing " + action + ". Exception sending report file " + file.getAbsolutePath() + " to user for download", e);
		}
	}

	static String updateShopPreferences(ShopDao shopDao, Shop shop, List<Exception> errors) {
		try {
			shopDao.updateShopPreferences(shop);
		} catch (Exception e) {
			errors.add(e);
			log.error("Error persisting shop preferences", e);
		}

		if (errors.isEmpty()) {
			return "{\"saved\":true}";
		} else {
			ElasticCharAppender msg = borrowBuilder();
			try {
				for (Exception e : errors) {
					if (e instanceof IllegalArgumentException) {
						msg.append(e.getMessage());
						msg.append('\n');
					}
				}

				if (msg.length() > 0) {
					throw new ValidationException(msg.toString());
				} else {
					throw new ValidationException("Internal error saving your data.");
				}
			} finally {
				releaseBuilder(msg);
			}
		}
	}

	public static void applyProxyTemplateValues(App app, String shop, ParameterizedString out){
		if (out.contains("SHOP")) {
			String domain = app.getShopDomain(shop);
			out.set("SHOP", domain);
		}
		if (out.contains("PROXY")) {
			if(app.isTestingLocally()){
				out.set("PROXY", ".");
			} else {
				out.set("PROXY", app.getShopifyProxy());
			}
		}
	}

	static ParameterizedString newTemplate(String path, String defaultValue) {
		ParameterizedString template = new ParameterizedString(readTextFromResource(path, StandardCharsets.UTF_8));
		if (template.contains("JS")) {
			template.set("JS", js);
		}
		template.setDefaultValue(defaultValue);
		return template;
	}
}
