

package com.univocity.shopify.exception;

/**
 * @author uniVocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 */
public class ValidationException extends IllegalStateException {

	private static final long serialVersionUID = 3910230397317922180L;

	public ValidationException(String message, Throwable cause) {
		super(message, cause);
	}

	public ValidationException(String s) {
		super(s);
	}

}
