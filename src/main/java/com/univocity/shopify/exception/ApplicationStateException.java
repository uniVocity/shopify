

package com.univocity.shopify.exception;

public class ApplicationStateException extends IllegalStateException {

	private static final long serialVersionUID = 4127194882186346596L;

	public ApplicationStateException(String s) {
		super(s);
	}

	public ApplicationStateException(String message, Throwable cause) {
		super(message, cause);
	}
}
