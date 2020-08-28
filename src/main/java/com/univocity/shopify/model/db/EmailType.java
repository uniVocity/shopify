package com.univocity.shopify.model.db;


import com.univocity.parsers.common.input.*;
import com.univocity.shopify.utils.*;
import org.apache.commons.lang3.*;
import org.slf4j.*;

import java.util.*;

import static com.univocity.shopify.model.db.DefaultEmailTemplates.*;
import static com.univocity.shopify.model.db.EmailType.Keys.*;
import static com.univocity.shopify.utils.Utils.*;

public enum EmailType {


	ORDER_PLACED(1, "Order placed", new String[]{QRCODE, PAYMENT_ADDRESS, TOKEN_SYMBOL}, new String[]{ORDER_ID, EXPIRATION_DATE, CHECKOUT_URL}),
	PAYMENT_RECEIVED(2, "Payment received", new String[]{ORDER_URL}, new String[]{ORDER_ID, TOKEN_SYMBOL, CHECKOUT_URL});

	private static final Logger log = LoggerFactory.getLogger(EmailType.class);

	private final String[] defaultTemplateKeys = new String[]{NAME, STORE_NAME, ORDER_AMOUNT};

	public final int code;
	private final Set<String> acceptedParameters;
	private final String[] mandatoryParameters;

	private final Set<String> acceptedParameterSet;
	private final Set<String> mandatoryParameterSet;
	public final String lowerCaseName;
	public final String titleFieldName;
	public final String bodyFieldName;
	public final String description;
	public final String acceptedParameterList;
	public final String mandatoryParameterList;

	EmailType(int type, String description, String[] mandatoryParameters, String[] acceptedParameters) {
		this.code = type;
		this.description = description;
		this.mandatoryParameters = mandatoryParameters;

		this.acceptedParameters = new HashSet<>();
		Collections.addAll(this.acceptedParameters, acceptedParameters);
		Collections.addAll(this.acceptedParameters, mandatoryParameters);
		Collections.addAll(this.acceptedParameters, defaultTemplateKeys);

		this.acceptedParameterSet = Collections.unmodifiableSet(new TreeSet<>(this.acceptedParameters));
		this.mandatoryParameterSet = Collections.unmodifiableSet(new TreeSet<>(Arrays.asList(this.mandatoryParameters)));
		this.lowerCaseName = this.name().toLowerCase();
		this.titleFieldName = lowerCaseName + "_title";
		this.bodyFieldName = lowerCaseName + "_body";

		this.mandatoryParameterList = StringUtils.join(mandatoryParameterSet, ',');

		TreeSet<String> tmp = new TreeSet<>(acceptedParameterSet);
		tmp.removeAll(mandatoryParameterSet);
		this.acceptedParameterList = StringUtils.join(tmp, ',');
	}

	public static EmailType fromCode(int code) {
		for (EmailType t : EmailType.values()) {
			if (t.code == code) {
				return t;
			}
		}
		throw new IllegalArgumentException("Unknown email type code: '" + code + "'");

	}

	public void validateParameters(ParameterizedString string, boolean validateMandatory) {
		Set<String> missing = new TreeSet<>();
		Set<String> unknown = new TreeSet<>();

		Set<String> params = string.getParameters();
		if (validateMandatory) {
			for (int i = 0; i < mandatoryParameters.length; i++) {
				if (!params.contains(mandatoryParameters[i])) {
					missing.add(mandatoryParameters[i]);
				}
			}
		}

		for (String param : params) {
			if (!acceptedParameters.contains(param)) {
				unknown.add(param);
			}
		}

		if (missing.isEmpty() && unknown.isEmpty()) {
			return;
		}

		ElasticCharAppender msg = borrowBuilder();
		try {
			msg.append("Invalid template: ");
			if (!missing.isEmpty()) {
				msg.append("\n\tMandatory parameters missing: ");
				appendSeparatedBy(msg, ",", missing);
			}
			if (!unknown.isEmpty()) {
				msg.append("\n\tUnknown parameters: ");
				appendSeparatedBy(msg, ",", unknown);
			}
		} finally {
			releaseBuilder(msg);
		}

		log.warn(msg.toString());

		throw new IllegalArgumentException(msg.toString());
	}

	public static class Keys {
		public static final String NAME = "NAME";
		public static final String ORDER_ID = "ORDER_ID";
		public static final String QRCODE = "QRCODE";
		public static final String PAYMENT_ADDRESS = "PAYMENT_ADDRESS";
		public static final String TOKEN_SYMBOL = "TOKEN_SYMBOL";
		public static final String ORDER_AMOUNT = "ORDER_AMOUNT";
		public static final String CHECKOUT_URL = "CHECKOUT_URL";
		public static final String ORDER_URL = "ORDER_URL";
		public static final String EXPIRATION_DATE = "EXPIRATION";
		public static final String STORE_NAME = "STORE_NAME";
	}

	public EmailTemplate getDefaultTemplate() {
		switch (this) {
			case ORDER_PLACED:
				return ORDER_PLACED_TEMPLATE.clone();
			case PAYMENT_RECEIVED:
				return PAYMENT_RECEIVED_TEMPLATE.clone();
			default:
				throw new IllegalStateException("Unknown email template for type " + this);

		}
	}

	public Set<String> getAcceptedParameters() {
		return acceptedParameterSet;
	}

	public Set<String> getMandatoryParameters() {
		return mandatoryParameterSet;
	}
}

class DefaultEmailTemplates {
	public static final EmailTemplate ORDER_PLACED_TEMPLATE;
	public static final EmailTemplate PAYMENT_RECEIVED_TEMPLATE;

	static {
		ORDER_PLACED_TEMPLATE = new EmailTemplate();
		ORDER_PLACED_TEMPLATE.setEmailType(EmailType.ORDER_PLACED);
		ORDER_PLACED_TEMPLATE.setTitle("Your order {{ " + ORDER_ID + " }} has been placed and is awaiting payment.");
		ORDER_PLACED_TEMPLATE.setBody("" +
				"Dear {{ " + NAME + " }},\r\n" +
				"\r\n" +
				"Your order has been received and is waiting for payment of {{ " + ORDER_AMOUNT + " }} {{ " + TOKEN_SYMBOL + " }} to the following address:\r\n" +
				"\r\n" +
				" * {{ " + PAYMENT_ADDRESS + " }}\r\n" +
				"\r\n" +
				"Or simply scan this QR code: {{ " + QRCODE + " }}\r\n" +
				"\r\n" +
				"The purchase price is guaranteed until until: {{ " + EXPIRATION_DATE + ", mmm dd, yyyy }}.\r\n" +
				"After that, the total payable will be updated according with the market price of {{ " + TOKEN_SYMBOL + " }}.\r\n" +
				"\r\n" +
				"Your order can be reviewed in the following link: {{ " + CHECKOUT_URL + " }}\r\n" +
				"This link will remain valid while payment is pending. If you already paid please disconsider this e-mail.\r\n" +
				"\r\n" +
				"Kind regards,\r\n" +
				"{{ " + STORE_NAME + " }}\r\n");

		PAYMENT_RECEIVED_TEMPLATE = new EmailTemplate();
		PAYMENT_RECEIVED_TEMPLATE.setEmailType(EmailType.PAYMENT_RECEIVED);
		PAYMENT_RECEIVED_TEMPLATE.setTitle("We received your payment for order {{ " + ORDER_ID + " }}. Waiting for confirmation.");
		PAYMENT_RECEIVED_TEMPLATE.setBody("" +
				"Dear {{ " + NAME + " }},\r\n" +
				"\r\n" +
				"Thank you for paying with {{ " + TOKEN_SYMBOL + " }}. We are waiting for blockchain confirmation and will process your order in a few minutes.\r\n" +
				"You can review your order on {{ " + ORDER_URL + " }}.\r\n" +
				"\r\n" +
				"Kind regards,\r\n" +
				"{{ " + STORE_NAME + " }}\r\n");
	}
}