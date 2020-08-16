

package com.univocity.shopify.model.db;



import com.univocity.shopify.utils.*;

import java.math.*;

public class SystemMessageRenderer extends MessageRenderer {

	private static final String CURRENT_BILL = "CURRENT_BILL";
	private static final String REMAINING_AMOUNT = "REMAINING_AMOUNT";
	private static final String APP_BILLING_URL = "APP_BILLING_URL";
	private static final String CAPPED_AMOUNT = "CAPPED_AMOUNT";
	private static final String TEXT = "TEXT";

	SystemMessageRenderer(ParameterizedString title, ParameterizedString body) {
		super(title, body);
	}

	public void setCurrentBill(BigDecimal billAmount) {
		set(CURRENT_BILL, Utils.toString(billAmount));
	}

	public void setRemainingAmount(BigDecimal remainingAmount) {
		set(REMAINING_AMOUNT, Utils.toString(remainingAmount));
	}

	public void setCappedAmount(BigDecimal cappedAmount) {
		set(CAPPED_AMOUNT, Utils.toString(cappedAmount));
	}

	public void setAppBillingUrl(String appBillingUrl) {
		set(APP_BILLING_URL, appBillingUrl);
	}

	public void setText(String text) {
		set(TEXT, text);
	}

}