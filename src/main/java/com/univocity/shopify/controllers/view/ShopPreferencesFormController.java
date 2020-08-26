package com.univocity.shopify.controllers.view;


import com.univocity.parsers.common.input.*;
import com.univocity.shopify.*;
import com.univocity.shopify.dao.*;
import com.univocity.shopify.email.*;
import com.univocity.shopify.exception.*;
import com.univocity.shopify.model.db.*;
import com.univocity.shopify.utils.*;
import org.apache.commons.lang3.*;
import org.apache.commons.validator.routines.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.mail.javamail.*;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.*;

import javax.mail.*;
import javax.mail.internet.*;
import javax.servlet.http.*;
import java.util.*;

import static com.univocity.shopify.controllers.view.ViewUtils.*;
import static com.univocity.shopify.utils.Utils.*;
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

	@Autowired
	EmailTemplateDao templates;

	static final String PREFERENCES_VIEW_ACTION = "preferences view";

	private static final ParameterizedString active_template;
	private static final ParameterizedString emailTemplate;
	private static final EmailType[] EMAIL_TYPES = EmailType.values();

	static {
		active_template = newTemplate("template/admin/preferences.html", "");
		emailTemplate = newTemplate("template/admin/email_template_tab.html", "");
	}

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
			ParameterizedString out = active_template.clone();

			applyProxyTemplateValues(app, shopName, out);

			out.set("REPLY_TO", shop.getReplyToAddress());
			out.set("NOTIFICATION_LIST", shop.getNotificationEmailList());
			out.set("USE_OWN_MAIL_SERVER_CHECKED", shop.getUseOwnMailServer() ? "checked" : "");

			out.set("SMTP_HOST", shop.getSmtpHost());
			out.set("SMTP_PORT", shop.getSmtpPort());
			out.set("SMTP_USER", shop.getSmtpUsername());

			char[] smtpPassword = shop.getSmtpPassword();
			out.set("SMTP_PASSWORD", smtpPassword == null ? "" : new String(smtpPassword));
			out.set("SMTP_TLS_SSL", shop.getSmtpTlsSsl());
			out.set("SMTP_TLS_SSL_CHECKED", shop.getSmtpTlsSsl() ? "checked" : "");
			out.set("SMTP_SENDER", shop.getSmtpSender());

			out.set("EMAIL_TEMPLATE_TABS", getEmailTemplateTabs(shop));

			return out.applyParameterValues();
		} catch (ApplicationStateException e) {
			return e.getMessage();
		}
	}

	private String getEmailTemplateTabs(Shop shop) {
		ElasticCharAppender out = Utils.borrowBuilder();
		try {
			ParameterizedString tab = emailTemplate.clone();

			for (EmailType emailType : EmailType.values()) {
				out.append(setEmailTemplateFields(tab, shop, emailType));
			}

			return out.toString();
		} finally {
			Utils.releaseBuilder(out);
		}
	}

	private String setEmailTemplateFields(ParameterizedString out, Shop shop, EmailType emailType) {
		EmailTemplate template = templates.getEmailTemplate(shop.getId(), emailType);
		out.set("EMAIL_TYPE", emailType.lowerCaseName);
		out.set("EMAIL_TYPE_DESCRIPTION", emailType.description);
		out.set("EMAIL_TITLE", template.getTitle());
		out.set("EMAIL_BODY", template.getBody());
		out.set("FIELDS", emailType.acceptedParameterList);
		out.set("MANDATORY_FIELDS", emailType.mandatoryParameterList);

		return out.applyParameterValues();
	}


	@CrossOrigin(origins = "*", methods = POST)
	@RequestMapping(value = "/preferences/save", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public String saveProductLicenseSettings(HttpServletRequest request) {
		return modifySettings(app, "save shop preferences", request, params -> savePreferenceChanges(request, params));
	}

	@CrossOrigin(origins = "*", methods = POST)
	@RequestMapping(value = "/preferences/mail/test", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public String testEmailPreferences(HttpServletRequest request) {
		return modifySettings(app, "test e-mail preferences", request, params -> testEmailPreferences(request, params));
	}

	@CrossOrigin(origins = "*", methods = POST)
	@RequestMapping(value = "/preferences/mail/test/template", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	@ResponseBody
	public String testEmailTemplate(HttpServletRequest request) {
		return modifySettings(app, "test e-mail template", request, params -> testEmailTemplate(request, params));
	}

	private void validateEmail(String email, String description) {
		if (StringUtils.isBlank(email)) {
			throw new ValidationException(StringUtils.capitalize(description) + " e-mail address must be provided");
		}
		email = email.trim();
		if (!EmailValidator.getInstance().isValid(email)) {
			throw new ValidationException("Invalid " + description + " e-mail: " + email);
		}
	}

	private void validateMandatoryParameter(String value, String description) {
		if (StringUtils.isBlank(value)) {
			throw new ValidationException(StringUtils.capitalize(description) + " must be provided");
		}
	}

	private void readMailServerProperties(Shop shop, HttpServletRequest request, boolean testingEmailServer) {
		if (!testingEmailServer) {
			validateEmail(request.getParameter("reply_to"), "reply-to");
			String list = request.getParameter("notification_list");
			if (StringUtils.isBlank(list)) {
				throw new ValidationException("System notification list must have at least one e-mail address");
			}

			for (String email : list.split(",")) {
				validateEmail(email, "system notification");
			}
		}

		if (Boolean.valueOf(request.getParameter("use_own_mail_server"))) {
			validateMandatoryParameter(request.getParameter("smtp_host"), "SMTP host");
			validateMandatoryParameter(request.getParameter("smtp_user"), "SMTP username");
			validateMandatoryParameter(request.getParameter("smtp_password"), "SMTP password");
			validateEmail(request.getParameter("smtp_sender"), "SMTP sender");
		}

		setBoolean(request, "use_own_mail_server", shop::setUseOwnMailServer);
		setString(request, "reply_to", shop::setReplyToAddress);
		setString(request, "notification_list", shop::setNotificationEmailList);
		setString(request, "smtp_sender", shop::setSmtpSender);
		setString(request, "smtp_host", shop::setSmtpHost);
		setInteger(request, "smtp_port", shop::setSmtpPort);
		setString(request, "smtp_user", shop::setSmtpUsername);
		setString(request, "smtp_password", shop::setSmtpPassword);
		setBoolean(request, "smtp_tls_ssl", shop::setSmtpTlsSsl);

	}

	private final String savePreferenceChanges(HttpServletRequest request, Map<String, String[]> params) {
		String shopName = params.get("shop")[0];
		Shop shop = shops.getShop(shopName);

		readMailServerProperties(shop, request, false);

		List<Exception> errors = new ArrayList<>();
		persistChanges(request, shop, errors);

		return ViewUtils.updateShopPreferences(shops, shop, errors);
	}

	private void persistChanges(HttpServletRequest request, Shop shop, List<Exception> errors) {
		for (EmailType templateType : EMAIL_TYPES) {

			EmailTemplate template = templates.getEmailTemplate(shop.getId(), templateType);

			try {
				setString(request, templateType.titleFieldName, template::setTitle);
			} catch (IllegalArgumentException e) {
				errors.add(e);
			}

			try {
				setString(request, templateType.bodyFieldName, template::setBody);
			} catch (IllegalArgumentException e) {
				errors.add(e);
			}

			if (template.getShopId() == null) {
				template.setShopId(shop.getId());
			}

			try {
				templates.persist(template);
			} catch (Exception e) {
				log.error("Error saving e-mail template " + templateType + " of shop " + shop.getId() + " (" + shop.getShopName() + ")", e);
				errors.add(e);
			}
		}
	}


	private final String testEmailPreferences(HttpServletRequest request, Map<String, String[]> params) {
		String shopName = params.get("shop")[0];
		Shop shop = shops.getShop(shopName);

		Shop testShop = new Shop();

		readMailServerProperties(testShop, request, true);

		testShop.setId(shop.getId());
		testShop.setShopName(shop.getShopName());
		testShop.setReplyToAddress(shop.getReplyToAddress());

		if (testShop.getUseOwnMailServer()) {
			if (!testShop.isConfigured()) {
				throw new ValidationException("E-mail server configuration is incomplete");
			}
		}

		ShopMailSender sender = testShop.getMailSender(systemMailSenderConfig);
		MailSenderConfig config = sender.getConfig();
		JavaMailSender mailSender = sender.getMailSender();

		List<MimeMessage> messages = new ArrayList<>();
		try {
			String title = "Test e-mail";
			String body = "Congratulations!\r\n\r\nYour e-mail settings are good to go!\r\n\r\nBest regards,\r\nThe univocity team\r\nhttp://www.univocity.com\r\n";

			if (!testShop.getUseOwnMailServer()) {
				Email email = sender.newEmail(shop.getId(), new String[]{shop.getOwnerEmail()}, title, body);
				email.setReplyTo(testShop.getReplyToAddress());
				if (!systemMailSender.sendEmailViaSmtp(email)) {
					messages.add(generateMessage(mailSender, shop, config, title, body));
				}
			} else {
				messages.add(generateMessage(mailSender, shop, config, title, body));
			}

			log.info("Test e-mails ready for sending to owner of shop " + shop.getId() + ". Sending template.");

			if (messages.size() > 0) {
				mailSender.send(messages.toArray(new MimeMessage[0]));
				log.info("Test e-mails sent to shop " + shop.getId() + ".");
			}
		} catch (Exception e) {
			Throwable cause = e.getCause();
			if (cause == null) {
				cause = e;
			}
			log.warn("Test e-mail sending failed for shop " + shop.getId(), e);
			throw new ValidationException(cause.getMessage());
		}

		return "{\"message\":\"E-mails sent to " + shop.getOwnerEmail() + "\"}";
	}


	private MimeMessage generateMessage(JavaMailSender mailSender, Shop shop, MailSenderConfig config, String title, String body) throws MessagingException {
		MimeMessage message = mailSender.createMimeMessage();

		MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

		helper.setFrom(config.getSmtpSender());

		helper.setTo(shop.getOwnerEmail());
		if (StringUtils.isNotBlank(config.getReplyToAddress())) {
			if (EmailValidator.getInstance().isValid(config.getReplyToAddress())) {
				throw new ValidationException("Reply-to address '" + config.getReplyToAddress() + "' is invalid");
			}
			helper.setReplyTo(config.getReplyToAddress());
		}

		helper.setSubject(title);
		helper.setText(body);

		return message;
	}

	private final String testEmailTemplate(HttpServletRequest request, Map<String, String[]> params) {
		String shopName = params.get("shop")[0];
		Shop shop = shops.getShop(shopName);

		String templateName = params.get("template")[0];
		String title = params.get("title")[0];
		String body = params.get("body")[0];

		try {
			for (EmailType templateType : EMAIL_TYPES) {
				if (templateType.lowerCaseName.equals(templateName)) {
					EmailTemplate template = templateType.getDefaultTemplate();
					template.setBody(body);
					template.setTitle(title);
					MessageRenderer renderer = template.renderer();

					renderer.setExpirationDate(java.sql.Date.valueOf("2015-12-25"));
					renderer.setStoreName(shop);
					renderer.setName(shop.getOwnerNameForEmailing());
					renderer.setCheckoutUrl("https://checkout.shopify.com/123123/invoices/61a2d718be6c2472816c3827c5a75fe4");
					renderer.setOrderUrl("https://" + shop.getDomain() + "/17122445/orders/f49c7024dba124a149d9ffe8f6");
					renderer.setQrCode("<QR_CODE_HERE>"); //TODO: generate QR code from payment address
					renderer.setPaymentAddress("addr1q9jap0f549256uvkmrwv2yq9vruphxqrm92szg03xgx4dhtdd0sqyslnjxvce9syyw4ktnrh0n7ct60zrs29wnef3jqqttmm2g");
					renderer.setOrderId("1001");
					renderer.setTokenSymbol("ADA");
					renderer.setOrderAmount("399.993412");

					title = "(TEST) " + renderer.renderTitle();
					body = renderer.renderBody();

					Email email = systemMailSender.newEmail(shop.getId(), new String[]{shop.getOwnerEmail()}, title, body);
					email.setReplyTo(shop.getReplyToAddress());
					if (!systemMailSender.sendEmailViaSmtp(email)) {
						log.warn("Could not send template test e-mail of type {} to owner of shop {}", templateType, shop.getId());
						return "{\"message\":\"Error sending test e-mail to " + shop.getOwnerEmail() + "\"}";
					}
					log.info("Sent template test e-mail of type {} to owner of shop {}", templateType, shop.getId());
				}
			}
		} catch (Exception e) {
			Throwable cause = e.getCause();
			if (cause == null) {
				cause = e;
			}
			log.warn("Test e-mail template sending failed for shop " + shop.getId(), e);
			throw new ValidationException(cause.getMessage());
		}

		return "{\"message\":\"Test e-mail sent to " + shop.getOwnerEmail() + "\"}";
	}
}
