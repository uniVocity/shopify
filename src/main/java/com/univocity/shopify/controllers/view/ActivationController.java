package com.univocity.shopify.controllers.view;

import com.univocity.shopify.*;
import com.univocity.shopify.controllers.api.*;
import com.univocity.shopify.dao.*;
import com.univocity.shopify.email.*;
import com.univocity.shopify.exception.*;
import com.univocity.shopify.model.db.*;
import com.univocity.shopify.utils.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.stereotype.*;
import org.springframework.transaction.support.*;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.*;
import java.util.*;

import static com.univocity.shopify.controllers.view.ViewUtils.*;
import static com.univocity.shopify.utils.Utils.*;
import static org.springframework.web.bind.annotation.RequestMethod.*;

@Controller
@Import(ApplicationConfiguration.class)
public class ActivationController {

	@Autowired
	App utils;

	@Autowired
	ShopDao shops;

	@Autowired
	SystemMailSender systemMailSender;

	@Autowired
	ShopifyApiService shopifyApiService;

	@Autowired
	TransactionTemplate transactionTemplate;

	private static final ParameterizedString activationTemplate;
	static final String ACTIVATE_APP_ACTION = "request blockchain payment activation";
	static final String AUTHORIZE_APP_ACTION = "cardano payment authorization";

	static {
		activationTemplate = newTemplate("template/admin/activation.html", "");
	}

	@CrossOrigin(origins = "*", methods = GET)
	@RequestMapping(value = "/blockchain/activation", produces =  {MediaType.TEXT_HTML_VALUE, MediaType.APPLICATION_JSON_VALUE})
	@ResponseBody
	public String requestBlockchainPaymentActivation(HttpServletRequest request) {
		return modifySettings(utils, ACTIVATE_APP_ACTION, request, this::requestBlockchainPaymentActivation);
	}

	private final String requestBlockchainPaymentActivation(Map<String, String[]> params) {
		try {
			String shopName = params.get("shop")[0];
			shops.getShop(shopName);
			ParameterizedString out = activationTemplate.clone();
			applyProxyTemplateValues(utils, shopName, out);
			return out.applyParameterValues();
		} catch (ApplicationStateException e) {
			return e.getMessage();
		}
	}


	@CrossOrigin(origins = "*", methods = POST)
	@RequestMapping(value = "/blockchain/authorize", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public String authorizeApp(HttpServletRequest request) {
		return modifySettings(utils, AUTHORIZE_APP_ACTION, request, this::authorizeApp);
	}

	private final String authorizeApp(Map<String, String[]> params) {
		String shopName = params.get("shop")[0];
		Shop shop = shops.getShop(shopName);

		shops.activateShop(shop);
		log.info("License agreement accepted by shop {}. Shop activated.", shopName);

		String url = Shop.getAdminUrl(shop.getShopName()) + "/apps/cardano";

		return "{\"url\":\"" + getEscapedUrl(url) + "\"}"; //navigate to preferences page.
	}
}
