package com.univocity.shopify.model.shopify.serializer;


import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.univocity.shopify.utils.*;

import java.io.*;
import java.time.*;


public final class ShopifyDateSerializer extends JsonSerializer<ZonedDateTime> {
	@Override
	public void serialize(final ZonedDateTime value, final JsonGenerator gen, final SerializerProvider arg2) throws IOException {
		gen.writeString(Utils.formatDateTime(value));
	}
}
