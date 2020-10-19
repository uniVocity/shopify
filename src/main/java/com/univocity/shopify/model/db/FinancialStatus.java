package com.univocity.shopify.model.db;

public enum FinancialStatus {
	pending(1),
	authorized(2),
	partially_paid(3),
	paid(4),
	partially_refunded(5),
	refunded(6),
	voided(7);

	public int code;

	FinancialStatus(int code) {
		this.code = code;
	}

	public boolean allowsBlockchainPayment() {
		return this == pending;
	}

	public static FinancialStatus fromCode(Integer code) {
		if (code == null) {
			return null;
		}
		int i = code;
		if (i <= 0) {
			return null;
		}
		for (FinancialStatus status : FinancialStatus.values()) {
			if (status.code == i) {
				return status;
			}
		}
		return null;
	}
}