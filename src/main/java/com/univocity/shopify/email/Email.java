

package com.univocity.shopify.email;


import com.univocity.shopify.utils.*;
import org.apache.commons.lang3.*;
import org.springframework.jdbc.core.*;

import java.sql.*;
import java.util.*;

import static com.univocity.shopify.utils.database.Converter.*;

public class Email implements RowMapper<Email> {

	private long id;
	private long shopId;
	private String from;
	private String[] to;
	private String replyTo;
	private String title;
	private String body;
	private long hash = 0L;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Email email = (Email) o;

		if (from != null ? !from.equals(email.from) : email.from != null) return false;
		// Probably incorrect - comparing Object[] arrays with Arrays.equals
		if (!Arrays.equals(to, email.to)) return false;
		if (replyTo != null ? !replyTo.equals(email.replyTo) : email.replyTo != null) return false;
		if (title != null ? !title.equals(email.title) : email.title != null) return false;
		return body != null ? body.equals(email.body) : email.body == null;
	}

	public long getHash() {
		if (hash == 0L) {
			hash = Utils.longHash(from, replyTo, title, body);
			hash = Utils.longHash(hash, to);
		}
		return hash;
	}

	@Override
	public String toString() {
		return "Email{" +
				"id=" + id +
				", shopId=" + shopId +
				", from='" + from + '\'' +
				", to=" + Arrays.toString(to) +
				", replyTo='" + replyTo + '\'' +
				", title='" + title + '\'' +
				", content='" + body + '\'' +
				'}';
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getShopId() {
		return shopId;
	}

	public void setShopId(long shopId) {
		this.shopId = shopId;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String[] getTo() {
		return to;
	}

	public String getToAddressList() {
		return Utils.concatenate(";", to);
	}

	public void setTo(String[] to) {
		this.to = to;
	}

	public String getReplyTo() {
		return replyTo;
	}

	public void setReplyTo(String replyTo) {
		this.replyTo = replyTo;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	@Override
	public Email mapRow(ResultSet rs, int rowNum) throws SQLException {
		Email out = new Email();
		out.id = readLong(rs, "id");
		out.shopId = readLong(rs, "shop_id");
		out.replyTo = rs.getString("reply_to_addr");
		out.to = StringUtils.split(rs.getString("to_addr"), ';');
		out.title = rs.getString("title");
		out.from = rs.getString("from_addr");
		out.body = rs.getString("body");
		return out;
	}
}
