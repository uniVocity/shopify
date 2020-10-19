

package com.univocity.shopify.dao;

import com.univocity.shopify.email.*;
import com.univocity.shopify.utils.*;
import com.univocity.shopify.utils.database.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.dao.*;

import java.util.*;

import static com.univocity.shopify.utils.database.ExtendedJdbcTemplate.*;

public class EmailQueueDao {

	private static final Logger log = LoggerFactory.getLogger(EmailQueueDao.class);

	@Autowired
	ExtendedJdbcTemplate db;

	@Autowired
	App app;

	private static final String insert = generateInsertStatement("email_queue", "shop_id", "hash", "from_addr", "to_addr", "reply_to_addr", "title", "body");

	public boolean addToQueue(Email email) {
		try {
			db.insert(insert, email.getShopId(), email.getHash(), email.getFrom(), email.getToAddressList(), email.getReplyTo(), email.getTitle(), email.getBody());
			return true;
		} catch (DuplicateKeyException e) {
			log.debug("Attempting to re-send duplicate e-mail: " + email + ". " + e.getMessage());
			return false;
		} catch (Exception e) {
			if (e.getMessage().contains("Duplicate entry ")) {
				log.debug("Attempting to re-send duplicate e-mail: " + email + ". " + e.getMessage());
				return false;
			}
			log.error("Could not add e-mail to queue: " + email + ". ", e);
			throw new IllegalStateException("Could not add e-mail to queue: " + email + ". ", e);
		}
	}

	public void markAsSent(Email email) {
		try {
			db.update("UPDATE email_queue SET sent = 1 WHERE id = ? AND shop_id = ?", new Object[]{email.getId(), email.getShopId()});
		} catch (Exception e) {
			log.error("Error marking e-mail as sent: " + email + ". ", e);
		}
	}

	public void clearSystemMessages() {
		clearSentMessages(" AND shop_id = 0");
	}

	public void clearUserMessages() {
		if (app.isDatabaseMySQL()) {
			clearSentMessages(" AND shop_id <> 0 AND (created_at IS NULL OR created_at > DATE_SUB(NOW(), INTERVAL 1 HOUR))");
		} else {
			clearSentMessages(" AND shop_id <> 0 AND (created_at IS NULL OR created_at > DATEADD(day, -1, CURRENT_DATE()))");
		}
	}

	public void clearAllMessages() {
		clearSentMessages("");
	}

	private void clearSentMessages(String filter) {
		try {
			int removed = db.update("DELETE FROM email_queue WHERE sent = 1" + filter);
			if(removed != 0) {
				log.info("Removed {} sent e-mails from email queue table", removed);
			}
		} catch (Exception e) {
			log.error("Error clearing up sent emails from email queue table", e);
		}
	}

	public long queueSize() {
		return db.queryForObject("SELECT COUNT(*) FROM email_queue WHERE sent = 0", Number.class).longValue();
	}

	public List<Email> pollEmailsToSend(int limit) {
		String query = "SELECT * FROM email_queue WHERE sent = 0 ORDER BY id";
		if (limit > 0) {
			query += " LIMIT " + limit;
		}
		return db.query(query, (rs, rowNum) -> new Email().mapRow(rs, rowNum));
	}

}
