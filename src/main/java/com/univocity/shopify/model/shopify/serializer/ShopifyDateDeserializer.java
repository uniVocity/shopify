package com.univocity.shopify.model.shopify.serializer;


import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers.*;
import com.univocity.shopify.utils.*;

import java.io.*;
import java.time.*;
import java.util.*;

public final class ShopifyDateDeserializer extends JsonDeserializer<ZonedDateTime> {

	@Override
	public ZonedDateTime deserialize(final JsonParser parser, final DeserializationContext context) throws IOException {
		final String date = parser.getText();
		try {
			return Utils.parseDateTime(date);
		} catch (Exception ex) {
			// Not worked, so let the default date serializer give it a try.
			Date out = DateDeserializer.instance.deserialize(parser, context);
			return Utils.toLocalDateTime(out);
		}
	}
}