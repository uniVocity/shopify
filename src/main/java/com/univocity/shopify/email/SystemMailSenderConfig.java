package com.univocity.shopify.email;


import com.univocity.shopify.utils.*;
import org.apache.commons.lang3.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;

import static com.univocity.shopify.utils.Utils.*;


/**
 * @author uniVocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 */
public class SystemMailSenderConfig implements MailSenderConfig {

	private static final Logger log = LoggerFactory.getLogger(SystemMailSenderConfig.class);

	@Autowired
	App utils;

	@Autowired
	PropertyBasedConfiguration config;

	@Autowired
	EmailQueue emailQueue;

	private String smtpHost;
	private Boolean smtpTlsSsl;
	private Integer smtpPort;
	private String smtpUsername;
	private char[] smtpPassword;
	private String smtpSender = null;
	private String[] notificationEmails = null;
	private String apiKey;
	private String mailgunDomain;
	private ParameterizedString mailgunUrl;
	private String replyToAddress;

	@Override
	public String getSmtpHost() {
		if (smtpHost == null) {
			smtpHost = utils.getRequiredProperty("smtp.host");
		}
		return smtpHost;
	}

	public String getMailgunDomain() {
		if (mailgunDomain == null) {
			mailgunDomain = StringUtils.substringAfter(getSmtpUsername(), "@");
		}
		return mailgunDomain;
	}


	public ParameterizedString getMailgunUrl(){
		if(mailgunUrl == null){
			mailgunUrl = new ParameterizedString("https://api.mailgun.net/v3/" + getMailgunDomain() + "/messages?from={FROM}&to={TO}&subject={SUBJECT}&text={TEXT}");
		}
		return mailgunUrl;
	}

	public String getApiKey() {
		if (apiKey == null) {
			apiKey = config.getProperty("mail.api.key", "");
		}
		return apiKey;
	}

	@Override
	public boolean getSmtpTlsSsl() {
		if (smtpTlsSsl == null) {
			smtpTlsSsl = Boolean.valueOf(config.getProperty("smtp.tls_ssl", "false"));
		}
		return smtpTlsSsl;
	}


	@Override
	public Integer getSmtpPort() {
		if (smtpPort == null) {
			try {
				smtpPort = Integer.parseInt(utils.getRequiredProperty("smtp.port"));
			} catch (Exception e) {
				log.error("'smtp.port' was not correctly set. Exiting");
				System.exit(0);
				return 0;
			}
		}
		return smtpPort;

	}

	@Override
	public String getReplyToAddress() {
		if (replyToAddress == null) {
			replyToAddress = utils.getRequiredProperty("reply.to.address");
		}
		return replyToAddress;
	}


	@Override
	public String getSmtpUsername() {
		if (smtpUsername == null) {
			smtpUsername = utils.getRequiredProperty("smtp.username");
		}
		return smtpUsername;
	}


	@Override
	public char[] getSmtpPassword() {
		if (smtpPassword == null) {
			smtpPassword = utils.getRequiredProperty("smtp.password").toCharArray();
		}
		return smtpPassword;
	}

	@Override
	public String getSmtpSender() {
		if (smtpSender == null) {
			smtpSender = utils.getRequiredProperty("smtp.sender");
		}
		return smtpSender;
	}


	@Override
	public String[] getNotificationEmails() {
		if (notificationEmails == null) {
			try {
				notificationEmails = config.getList("notification.emails").toArray(new String[0]);
			} catch (Exception e) {
				log.warn("notification.emails was not set. Default to blank", e);
				notificationEmails = EMPTY_STRING_ARRAY;
			}
		}
		return notificationEmails;
	}

	@Override
	public EmailQueue getEmailQueue() {
		return emailQueue;
	}

	@Override
	public boolean isUsingSystemMailServer() {
		return true;
	}
}
