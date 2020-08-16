

package com.univocity.shopify.email;


import org.apache.commons.lang3.*;

public interface MailSenderConfig {
	String getSmtpHost();

	boolean getSmtpTlsSsl();

	Integer getSmtpPort();

	String getSmtpUsername();

	char[] getSmtpPassword();

	String getSmtpSender();

	String[] getNotificationEmails();

	String getReplyToAddress();

	EmailQueue getEmailQueue();

	default boolean isConfigured() {
		return StringUtils.isNoneBlank(getSmtpUsername(), getSmtpSender(), getSmtpHost()) && ArrayUtils.isNotEmpty(getSmtpPassword());
	}

	boolean isUsingSystemMailServer();
}
