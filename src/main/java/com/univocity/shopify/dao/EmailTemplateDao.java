/*
 * Copyright (c) 2017 Univocity Software Pty Ltd. All rights reserved.
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 *
 */

package com.univocity.shopify.dao;


import com.univocity.shopify.dao.base.*;
import com.univocity.shopify.exception.*;
import com.univocity.shopify.model.db.*;

public class EmailTemplateDao extends ShopEntityDao<EmailTemplate> {

	private static final String SELECT_TEMPLATE_BY_TYPE = "SELECT * FROM email_template WHERE shop_id = ? AND type = ? AND deleted_at IS NULL";

	public EmailTemplateDao() {
		super("email_template");

		enableCacheFor(SELECT_TEMPLATE_BY_TYPE, template -> new Object[]{template.getShopId(), template.getEmailType().code});
	}

	public EmailTemplate getEmailTemplate(Long shopId, EmailType emailType) {
		EmailTemplate out = queryForOptionalEntity(SELECT_TEMPLATE_BY_TYPE, shopId, emailType.code);
		if (out == null) {
			return emailType.getDefaultTemplate();
		}
		return out;
	}

	@Override
	protected EmailTemplate newEntity() {
		return new EmailTemplate();
	}

	@Override
	protected void validate(Operation operation, EmailTemplate entity) {
		if (entity.getEmailType() == null) {
			throw new ValidationException("E-mail template type cannot be null");
		}
	}
}
