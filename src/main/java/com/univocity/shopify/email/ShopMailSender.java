

package com.univocity.shopify.email;


import com.univocity.shopify.model.db.*;
import org.apache.commons.lang3.*;

public class ShopMailSender extends AbstractMailSender {

	private final Shop shop;
	private final MailSenderConfig config;

	public ShopMailSender(Shop shop, MailSenderConfig config) {
		this.shop = shop;
		this.config = config;
	}

	@Override
	public MailSenderConfig getConfig() {
		return config;
	}

	public boolean sendEmailToShopOwners(String title, String content) {
		if (ArrayUtils.isNotEmpty(getAdminMailList())) {
			return this.sendEmail(shop.getId(), getAdminMailList(), title, content);
		}
		return false;
	}
}
