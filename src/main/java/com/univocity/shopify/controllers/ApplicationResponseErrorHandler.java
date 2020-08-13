/*
 * Copyright (c) 2017 Univocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 *
 */

package com.univocity.shopify.controllers;

import com.univocity.shopify.exception.*;
import org.apache.commons.io.*;
import org.apache.commons.lang3.*;
import org.slf4j.*;
import org.springframework.http.*;
import org.springframework.http.client.*;
import org.springframework.web.client.*;

import java.io.*;
import java.nio.charset.*;

/**
 * @author uniVocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 */
public class ApplicationResponseErrorHandler implements ResponseErrorHandler {

	private static final Logger log = LoggerFactory.getLogger(ApplicationResponseErrorHandler.class);

	@Override
	public boolean hasError(ClientHttpResponse response) throws IOException {
		return isError(response.getStatusCode());
	}

	public static boolean isError(HttpStatus status) {
		HttpStatus.Series series = status.series();
		return (HttpStatus.Series.CLIENT_ERROR.equals(series) || HttpStatus.Series.SERVER_ERROR.equals(series));
	}

	@Override
	public void handleError(ClientHttpResponse response) throws IOException {
		String body = "";
		try {
			body = IOUtils.toString(response.getBody(), StandardCharsets.UTF_8);
		} catch (Exception ex) {
			response.close();
			//can't read body, ignore.
		}
		if (StringUtils.isNotBlank(body)) {
			throw new ShopifyErrorException(response.getStatusCode(), response.getStatusText(), body);
		} else {
			log.error("Response error {} ({}): {}", response.getStatusCode(), response.getStatusText(), body);
		}
	}
}
