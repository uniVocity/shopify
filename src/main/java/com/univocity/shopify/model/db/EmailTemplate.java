package com.univocity.shopify.model.db;


import com.univocity.shopify.model.db.core.*;
import com.univocity.shopify.utils.*;

import java.sql.*;
import java.util.*;

public class EmailTemplate extends ShopEntity<EmailTemplate> implements Cloneable {

	private EmailType emailType;
	private ParameterizedString title;
	private ParameterizedString body;

	public EmailTemplate() {
	}

	public EmailTemplate(EmailType emailType) {
		Utils.notNull(emailType, "Email template type");
		this.emailType = emailType;
	}


	@Override
	protected Set<String> getColumnsToIgnoreOnToString() {
		Set<String> out = super.getColumnsToIgnoreOnToString();
		Collections.addAll(out, "title", "body");
		return out;
	}

	@Override
	protected void addToMap(Map<String, Object> map) {
		map.put("type", emailType.code);
		map.put("title", title.toString());
		map.put("body", body.toString());
	}

	@Override
	protected void readFromRow(ResultSet rs, int rowNum) throws SQLException {
		emailType = EmailType.fromCode(rs.getInt("type"));
		title = toValidatedParameterizedString(rs.getString("title"), false);
		body = toValidatedParameterizedString(rs.getString("body"), false);
	}

	public EmailType getEmailType() {
		return emailType;
	}

	public void setEmailType(EmailType emailType) {
		this.emailType = emailType;
	}

	public String getTitle() {
		return title.toString();
	}

	public void setTitle(String title) {
		this.title = toValidatedParameterizedString(title, false);
	}

	public String getBody() {
		return body.toString();
	}

	public void setBody(String body) {
		this.body = toValidatedParameterizedString(body, true);
	}

	private ParameterizedString toValidatedParameterizedString(String string, boolean validateMandatory) {
		ParameterizedString out = new ParameterizedString(string, "{{", "}}");
		if (emailType != null) {
			emailType.validateParameters(out, validateMandatory);
		}
		out.setDefaultValue(MessageRenderer.NULL);
		return out;
	}

	public MessageRenderer renderer() {
		return new MessageRenderer(this.title, this.body);
	}

	public EmailTemplate clone() {
		EmailTemplate out = super.clone();
		out.title = this.title.clone();
		out.body = this.body.clone();
		return out;
	}
}

