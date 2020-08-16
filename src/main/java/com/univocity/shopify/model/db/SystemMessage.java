

package com.univocity.shopify.model.db;


import com.univocity.shopify.model.db.core.*;
import com.univocity.shopify.utils.*;

import java.sql.*;
import java.util.*;

import static com.univocity.shopify.utils.Utils.*;


public class SystemMessage extends BaseEntity<SystemMessage> {

	private MessageType messageType;
	private ParameterizedString title;
	private ParameterizedString body;

	public SystemMessage() {
	}

	public SystemMessage(MessageType messageType) {
		notNull(messageType, "Message template type");
		this.messageType = messageType;
	}


	@Override
	protected Set<String> getColumnsToIgnoreOnToString() {
		Set<String> out = super.getColumnsToIgnoreOnToString();
		Collections.addAll(out, "title", "body");
		return out;
	}

	@Override
	protected void fillMap(Map<String, Object> map) {
		map.put("type", messageType.code);
		map.put("title", title.toString());
		map.put("body", body.toString());
	}

	@Override
	protected void readRow(ResultSet rs, int rowNum) throws SQLException {
		setTitle(rs.getString("title"));
		setBody(rs.getString("body"));
		setMessageType(MessageType.fromCode(rs.getInt("type")));
	}

	public MessageType getMessageType() {
		return messageType;
	}

	public void setMessageType(MessageType messageType) {
		this.messageType = messageType;
	}

	public String getTitle() {
		return title.toString();
	}

	public void setTitle(String title) {
		this.title = new ParameterizedString(title);
	}

	public String getBody() {
		return body.toString();
	}

	public void setBody(String body) {
		this.body = new ParameterizedString(body);
	}

	public SystemMessageRenderer renderer() {
		return new SystemMessageRenderer(this.title, this.body);
	}
}