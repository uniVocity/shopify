package com.univocity.shopify.exception;

import org.apache.commons.lang3.*;
import org.springframework.http.*;
import org.springframework.web.client.*;

/**
 * @author uniVocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 */
public class ShopifyErrorException extends HttpServerErrorException {

	private static final long serialVersionUID = 4019126525014809821L;
	private final String body;

	public ShopifyErrorException(HttpStatus statusCode, String statusText, String body) {
		super(statusCode, statusText + (StringUtils.isBlank(body) ? "" : ":\n" + body));
		this.body = body;
	}

	public String getBody() {
		return body;
	}
}
