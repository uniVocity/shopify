package com.univocity.shopify.email;

/**
 * @author uniVocity Software Pty Ltd - <a href="mailto:dev@univocity.com">dev@univocity.com</a>
 */
public interface MailSender {

	boolean sendEmail(long shopId, String[] to, String title, String content);

}
