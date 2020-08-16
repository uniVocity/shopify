

package com.univocity.shopify.email;

import com.univocity.parsers.common.input.*;
import com.univocity.shopify.dao.*;
import com.univocity.shopify.model.db.*;
import com.univocity.shopify.utils.*;
import org.apache.commons.lang3.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.mail.*;
import org.springframework.mail.javamail.*;
import org.springframework.scheduling.annotation.*;

import javax.mail.internet.*;
import java.util.*;

public class EmailQueue {

	private static final Logger log = LoggerFactory.getLogger(EmailQueue.class);

	@Autowired
	EmailQueueDao emailQueueDao;

	@Autowired
	App utils;

	@Autowired
	ShopDao shopDao;

	@Autowired
	SystemMailSender systemMailSender;

	@Autowired
	SystemMailSenderConfig systemMailSenderConfig;

	//invoked every 2 minutes with a fixed delay. Measured from the completion time of each preceding invocation.
	@Scheduled(fixedDelay = 120000)
	public void processAllEmails() {
		long queueSize = emailQueueDao.queueSize();
		if (queueSize > 50) {
			log.warn("E-mail queue size getting too large: {}. Processing all emails in one go.", queueSize);
			processEmails(0);
			emailQueueDao.clearAllMessages();
		}
	}

	//invoked every 5 seconds with a fixed delay. Measured from the completion time of each preceding invocation.
	@Scheduled(fixedDelay = 5000)
	public void processEmails() {
		processEmails(20);
	}

	//invoked every 5 minutes with a fixed delay. Measured from the completion time of each preceding invocation.
	@Scheduled(fixedDelay = 5 * 60 * 1000)
	public void clearSystemEmails() {
		emailQueueDao.clearSystemMessages();
	}

	//invoked every hour with a fixed delay. Measured from the completion time of each preceding invocation.
	@Scheduled(fixedDelay = 60 * 60 * 1000)
	public void clearUserEmails() {
		emailQueueDao.clearUserMessages();
	}

	private synchronized void processEmails(int amount) {
		List<Email> emails = emailQueueDao.pollEmailsToSend(amount);
		for (Email email : emails) {
			try {
				if (utils.isTestingLocally()) {
					log.info("Would send e-mail '" + email.getTitle() + "' to: " + email.getToAddressList() + " with text:\n" + email.getBody());
				} else {
					sendEmail(email);
				}
			} catch (MailException e) {
				log.warn("Unable to send e-mail '" + email.getTitle() + "' to: " + email.getToAddressList(), e);
			} finally {
				emailQueueDao.markAsSent(email);
			}
		}
	}

	public boolean offer(Email email) {
		return emailQueueDao.addToQueue(email);
	}

	private void sendEmail(Email email) throws MailException {
		boolean usingSystemServer = false;
		final JavaMailSender mailSender;
		long shopId = email.getShopId();
		Shop shop = null;
		if (shopId == 0L) {
			usingSystemServer = true;
			mailSender = systemMailSender.getMailSender();
		} else {
			try {
				shop = shopDao.getShop(shopId);
				usingSystemServer = shop.isUsingSystemMailServer();
			} catch (Exception e) {
				log.warn("Can't send e-mail for unknown shop ID " + shopId, e);
				return;
			}
			ShopMailSender shopSender = shop.getMailSender(systemMailSenderConfig);
			if (shopSender != null) {
				mailSender = shopSender.getMailSender();
			} else {
				mailSender = null;
			}
		}

		sendEmailViaSmtp(mailSender, email, shop);
		log.debug("SMTP e-mail '{}' sent successfully to: {}", email.getTitle(), email.getToAddressList());
	}

	public void sendEmailViaSmtp(final JavaMailSender mailSender, Email email, Shop shop) {
		if (mailSender == null) {
			log.warn("Can't send e-mail: " + email + ". No mail sender available");
			return;
		}

		MimeMessage message = mailSender.createMimeMessage();
		try {
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

			helper.setFrom(email.getFrom());
			helper.setTo(email.getTo());
			if (StringUtils.isNotBlank(email.getReplyTo())) { //populated via shop.getReplyToAddress()
				helper.setReplyTo(email.getReplyTo());
			}

			helper.setSubject(email.getTitle());
			helper.setText(email.getBody());
		} catch (javax.mail.MessagingException e) {
			log.warn("Unable to write e-mail", e);
		}

		try {
			mailSender.send(message);
		} catch (MailException e) {
			log.warn("Error sending e-mail", e);
			if (shop != null && shop.isConfigured() && shop.getUseOwnMailServer()) {
				shop.setUseOwnMailServer(false);

				String title = "Error sending e-mail from cardano shopify app server. Check your configuration.";

				String body;
				ElasticCharAppender msg = Utils.borrowBuilder();
				try {
					msg.append("We tried sending an e-mail on your behalf to " + email.getTo() + " but got an error from your server. Configuration details:");

					JavaMailSenderImpl sender = ((JavaMailSenderImpl) mailSender);
					Properties props = sender.getJavaMailProperties();

					msg.append("\r\n* Host: ");
					msg.append(sender.getHost());
					msg.append(':');
					msg.append((Object) sender.getPort());
					msg.append("\r\n* Username: ");
					msg.append(sender.getUsername());

					if (props.getProperty("mail.smtp.starttls.enable") != null) {
						msg.append("\r\n* TLS/SSL enabled: ");
						msg.append(props.getProperty("mail.smtp.starttls.enable"));
					}

					msg.append("\r\n\r\nYour e-mail server got disabled temporarily and our own server will be used to send e-mails until you re-enable it in the settings page: https://");
					msg.append(shop.getShopName());
					msg.append("/");
					msg.append(utils.getShopifyProxy());
					msg.append("/settings");
					msg.append("\r\nIn the meantime your clients will receive emails from licenses@univocity.com");

					if (StringUtils.isNotBlank(email.getReplyTo())) { //populated via shop.getReplyToAddress()
						msg.append(", with the \"reply-to\" header addressed to ");
						msg.append(email.getReplyTo());
					}
					msg.append('.');
					body = msg.toString();
				} finally {
					Utils.releaseBuilder(msg);
				}
				systemMailSender.sendErrorEmailToShopOwner(shop.getNotificationEmails(), title, body, e);
			}
		}
	}
}
