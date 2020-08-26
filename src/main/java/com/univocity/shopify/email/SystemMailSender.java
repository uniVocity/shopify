package com.univocity.shopify.email;


import com.univocity.shopify.model.db.*;
import com.univocity.shopify.utils.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;

import java.util.*;

public class SystemMailSender extends AbstractMailSender {

	private static final Logger log = LoggerFactory.getLogger(SystemMailSender.class);

	@Autowired
	private SystemMailSenderConfig config;

	@Autowired
	private App app;

	@Override
	public MailSenderConfig getConfig() {
		return config;
	}

	@Autowired
	EmailQueue emailQueue;

	public boolean sendEmail(String[] to, String title, String content) {
		if (app.isLive()) {
			return sendEmail(0L, to, title, content);
		} else {
			StringBuilder tmp = new StringBuilder();
			tmp.append("Test e-mail message to " + Arrays.toString(to)).append('\n');
			tmp.append(title).append('\n');
			tmp.append("-----------------------------------------------\n");
			tmp.append(content).append('\n');
			tmp.append("-----------------------------------------------\n");
			log.debug(tmp.toString());
		}
		return false;
	}

	public boolean sendEmail(String title, String content) {
		return sendEmail(getAdminMailList(), App.getServerName() + " - " + title, content);
	}

	public boolean sendErrorEmail(String title, String content, Throwable exception) {
		content = content + ">>>> Exception:\r\n" + Utils.shortenStackTrace(Utils.printStackTrace(exception), 12);
		return sendEmail(getAdminMailList(), App.getServerName() + " - " + title, content);
	}

	public boolean sendErrorEmailToShopOwner(String[] owners, String title, String content, Throwable exception) {
		sendErrorEmail(title, content, exception);

		content = content + "\r\n\r\nServer error:\r\n" + exception.getMessage();
		return sendEmail(Utils.join(getAdminMailList(), owners), App.getServerName() + " - " + title, content);
	}

	public boolean sendEmailViaSmtp(Email email) {
		try {
			emailQueue.sendEmail(email);
			return true;
		} catch (Exception e) {
			log.error("Error sending e-mail. Shop: " + email.getShopId(), e);
		}
		return false;
	}
}
