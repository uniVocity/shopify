

package com.univocity.shopify.email;

import org.apache.commons.lang3.*;
import org.slf4j.*;
import org.springframework.mail.javamail.*;

import javax.mail.*;
import java.util.*;

abstract class AbstractMailSender implements MailSender {
	private static final Logger log = LoggerFactory.getLogger(AbstractMailSender.class);

	private JavaMailSender mailSender;
	private String[] adminMailList;

	public abstract MailSenderConfig getConfig();

	public boolean isConfigured() {
		MailSenderConfig config = getConfig();
		return config != null && config.isConfigured();
	}

	public Email newEmail(long shopId, String[] to, String title, String content) {
		if (ArrayUtils.isEmpty(to)) {
			log.warn("Unable to send email. No recipient emails provided.");
			return null;
		}

		Email email = new Email();
		email.setReplyTo(getConfig().getReplyToAddress());
		email.setBody(content);
		email.setFrom(getConfig().getSmtpSender());
		email.setTitle(title);
		email.setTo(to);
		email.setShopId(shopId);
		return email;
	}

	@Override
	public boolean sendEmail(long shopId, String[] to, String title, String content) {
		Email email = newEmail(shopId, to, title, content);
		if(email == null){
			return false;
		}

		if (!isConfigured()) {
			log.warn("Unable to send email. No email configuration available.");
			return false;
		}

		EmailQueue queue = getConfig().getEmailQueue();
		if (queue == null) {
			log.warn("Unable to send email. E-mail queue undefined.");
			return false;
		}

		return queue.offer(email);
	}

	public JavaMailSender getMailSender() {
		if (mailSender == null && isConfigured()) {
			synchronized (this) {
				if (mailSender == null) {
					JavaMailSenderImpl sender = new JavaMailSenderImpl();
					sender.setHost(getConfig().getSmtpHost());

					Properties properties = new Properties();
					properties.put("mail.transport.protocol", "smtps");
					if (getConfig().getSmtpTlsSsl()) {
						properties.put("mail.smtps.starttls.enable", true);
					}
					properties.put("mail.smtps.auth", true);

					Integer port = getConfig().getSmtpPort();
					if (port != null && port != 0) {
						properties.put("mail.smtps.socketFactory.port", port);
						sender.setPort(port);
					}

					properties.put("mail.smtps.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
					properties.put("mail.smtps.socketFactory.fallback", false);

					final String user = getConfig().getSmtpUsername();
					final char[] password = getConfig().getSmtpPassword();

					Session session = Session.getInstance(properties, new Authenticator() {
						@Override
						protected PasswordAuthentication getPasswordAuthentication() {
							return new PasswordAuthentication(user, password == null ? null : new String(password));
						}
					});

					sender.setSession(session);
					mailSender = sender;
				}
			}
		}
		return mailSender;
	}

	protected String[] getAdminMailList() {
		if (adminMailList == null) {
			adminMailList = getConfig().getNotificationEmails();
		}
		return adminMailList;
	}

}
