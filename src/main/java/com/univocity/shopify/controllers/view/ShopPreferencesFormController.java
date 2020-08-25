package com.univocity.shopify.controllers.view;


import com.univocity.shopify.*;
import com.univocity.shopify.dao.*;
import com.univocity.shopify.email.*;
import com.univocity.shopify.exception.*;
import com.univocity.shopify.model.db.*;
import com.univocity.shopify.utils.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.*;

import javax.servlet.http.*;
import java.util.*;

import static com.univocity.shopify.controllers.view.ViewUtils.*;
import static org.springframework.web.bind.annotation.RequestMethod.*;

@Controller
@Import(ApplicationConfiguration.class)
public class ShopPreferencesFormController {

	@Autowired
	App app;

	@Autowired
	ShopDao shops;

	@Autowired
	SystemMailSenderConfig systemMailSenderConfig;


	@Autowired
	SystemMailSender systemMailSender;

	static final String PREFERENCES_VIEW_ACTION = "preferences view";

	@CrossOrigin(origins = "*", methods = GET)
	@RequestMapping(value = "/")
	@ResponseBody
	public RedirectView productLicenseSettings(@RequestParam("shop") String shopName, HttpServletRequest request) {
		RedirectView redirectView = new RedirectView();

		redirectView.setUrl(app.getEndpoint(shopName, "/preferences"));

		return redirectView;
	}

	@CrossOrigin(origins = "*", methods = GET)
	@RequestMapping(value = "/preferences", produces = {MediaType.TEXT_HTML_VALUE, MediaType.APPLICATION_JSON_VALUE})
	@ResponseBody
	public String productConfig(@RequestParam("shop") String shopName, HttpServletRequest request) {
		return modifySettings(app, PREFERENCES_VIEW_ACTION, request, params -> showPreferences(request, params));
	}

	private String showPreferences(HttpServletRequest request, Map<String, String[]> params) {
		String shopName = params.get("shop")[0];
		Shop shop = shops.getShop(shopName);
		try {
			return "<html>\n" +
					"<body>testing preferences</body>\n" +
					"</html>";

		} catch (ApplicationStateException e) {
			return e.getMessage();
		}
	}
//
//	@CrossOrigin(origins = "*", methods = POST)
//	@RequestMapping(value = "/preferences/save", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
//	@ResponseBody
//	public String saveProductLicenseSettings(HttpServletRequest request) {
//		return modifySettings(app, "save shop preferences", request, params -> savePreferenceChanges(request, params));
//	}

}
