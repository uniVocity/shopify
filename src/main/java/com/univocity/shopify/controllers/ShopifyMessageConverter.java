/*
 * Copyright (c) 2017 Univocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 *
 */

package com.univocity.shopify.controllers;

import com.fasterxml.jackson.databind.*;
import org.apache.commons.io.*;
import org.springframework.http.*;
import org.springframework.http.converter.*;
import org.springframework.http.converter.json.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import static java.nio.charset.StandardCharsets.*;

/**
 * @author uniVocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 */
public class ShopifyMessageConverter extends MappingJackson2HttpMessageConverter {

	private static final List<MediaType> types = Arrays.asList(
			new MediaType("text", "html", UTF_8),
			new MediaType("application", "json", UTF_8),
			new MediaType("application", "*+json", UTF_8)
	);

	private static ObjectMapper getMapper() {
		ObjectMapper om = new ObjectMapper();
//		om.configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true);
//		om.configure(SerializationFeature.WRAP_ROOT_VALUE, true);
		om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		return om;
	}

	public ShopifyMessageConverter() {
		super(getMapper());
		super.setSupportedMediaTypes(types);
	}

	public Object read(Type type, Class<?> contextClass, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
		if (contextClass == null && type != null && "java.lang.String".equals(type.getTypeName())) {
			return IOUtils.toString(inputMessage.getBody(), UTF_8);
		} else {
			return super.read(type, contextClass, inputMessage);
		}
	}

//	@Override
//	protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
//		if (clazz == String.class) {
//			return IOUtils.toString(inputMessage.getBody(), "UTF-8");
//		} else {
//			return super.readInternal(clazz, inputMessage);
//		}
//	}

	@Override
	protected void writeInternal(Object object, Type type, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
		if (object instanceof String) {
			outputMessage.getBody().write(((String) object).getBytes(UTF_8));
		} else {
			super.writeInternal(object, type, outputMessage);
		}
	}
}
