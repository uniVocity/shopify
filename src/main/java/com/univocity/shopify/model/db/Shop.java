package com.univocity.shopify.model.db;

import com.univocity.shopify.email.*;
import com.univocity.shopify.model.db.core.*;
import com.univocity.shopify.utils.*;
import org.apache.commons.lang3.*;
import org.slf4j.*;

import java.sql.*;
import java.util.*;

import static com.univocity.shopify.utils.database.Converter.*;

public class Shop extends BaseEntity<Shop> implements MailSenderConfig, Comparable<Shop> {

	private static final Logger log = LoggerFactory.getLogger(Shop.class);

	private String shopName;
	private String shopToken;
	private Boolean invalid;
	private Boolean useOwnEmailServer;

	private String smtpHost;
	private Boolean smtpTlsSsl;
	private Integer smtpPort;
	private String smtpUsername;
	private String encodedSmtpPassword;
	private String smtpSender;
	private String replyToAddress;
	private String notificationEmailList;

	private String shopAuth;

	private ShopMailSender mailSender;

	private String ownerEmail;
	private String customerEmail;
	private String domain;
	private String phone;
	private String shopOwner;
	private String timezone;
	private String country;
	private String city;
	private Long shopifyId;

	private Boolean active;

	private EmailQueue emailQueue;

	@Override
	protected void fillMap(Map<String, Object> map) {
		if (invalid == Boolean.TRUE) {
			setShopToken(null);
		}

		map.put("invalid", invalid);
		map.put("shop_name", getShopName());
		map.put("shop_token", getShopToken());

		map.put("customer_email", getCustomerEmail());
		map.put("domain", getDomain());
		map.put("phone", getPhone());
		map.put("owner_name", getShopOwnerName());
		map.put("timezone", getTimezone());
		map.put("country", getCountry());
		map.put("city", getCity());
		map.put("owner_email", getOwnerEmail());
		map.put("shopify_id", getShopifyId());

		map.put("smtp_host", getSmtpHost());
		map.put("smtp_tls_ssl", getSmtpTlsSsl());
		map.put("smtp_port", getSmtpPort());
		map.put("smtp_username", getSmtpUsername());
		map.put("smtp_password", getEncodedSmtpPassword());
		map.put("smtp_sender", getSmtpSender());
		map.put("email_notification_list", getNotificationEmailList());
		map.put("reply_to_address", getReplyToAddress());

		map.put("use_own_email_server", getUseOwnMailServer());
		map.put("active", isActive());
	}

	@Override
	protected void readRow(ResultSet rs, int rowNum) throws SQLException {
		setInvalid(rs.getBoolean("invalid"));
		setUseOwnMailServer(rs.getBoolean("use_own_email_server"));
		setShopName(rs.getString("shop_name"));
		setShopToken(rs.getString("shop_token"));

		setCustomerEmail(rs.getString("customer_email"));
		setDomain(rs.getString("domain"));
		setPhone(rs.getString("phone"));
		setShopOwner(rs.getString("owner_name"));
		setTimezone(rs.getString("timezone"));
		setCountry(rs.getString("country"));
		setCity(rs.getString("city"));
		setOwnerEmail(rs.getString("owner_email"));
		setShopifyId(readLong(rs, "shopify_id"));

		setSmtpHost(rs.getString("smtp_host"));
		setSmtpTlsSsl(readBoolean(rs, "smtp_tls_ssl"));
		setSmtpPort(readInteger(rs, "smtp_port"));
		setSmtpUsername(rs.getString("smtp_username"));
		encodedSmtpPassword = rs.getString("smtp_password");
		setSmtpSender(rs.getString("smtp_sender"));
		setNotificationEmailList(rs.getString("email_notification_list"));
		setReplyToAddress(rs.getString("reply_to_address"));
		if (getInvalid()) {
			setShopToken(null);
		}
		setActive(readBoolean(rs, "active"));
	}

	public String getShopName() {
		return shopName;
	}

	public void setShopName(String shopName) {
		this.shopName = shopName.toLowerCase();
	}

	public String getOwnerEmail() {
		return ownerEmail;
	}

	public void setOwnerEmail(String ownerEmail) {
		this.ownerEmail = ownerEmail;
	}

	public Long getShopifyId() {
		return shopifyId;
	}

	public void setShopifyId(Long shopifyId) {
		this.shopifyId = shopifyId;
	}

	public String getShopToken() {
		return shopToken;
	}

	public void setShopToken(String shopToken) {
		this.shopToken = shopToken;
		if (shopToken == null) {
			shopAuth = null;
		}
	}

	public Boolean getInvalid() {
		return invalid;
	}

	public void setInvalid(Boolean invalid) {
		this.invalid = invalid;
	}


	public String getShopAuth() {
		return shopAuth;
	}

	public void setShopAuth(String shopAuth) {
		this.shopAuth = shopAuth;
	}

	public String getSmtpHost() {
		return smtpHost;
	}

	public void setSmtpHost(String smtpHost) {
		this.mailSender = null;
		this.smtpHost = smtpHost;
	}

	public Integer getSmtpPort() {
		return smtpPort;
	}

	public void setSmtpPort(Integer smtpPort) {
		this.mailSender = null;
		this.smtpPort = smtpPort;
	}

	public boolean getSmtpTlsSsl() {
		return smtpTlsSsl == null ? false : smtpTlsSsl;
	}

	public void setSmtpTlsSsl(Boolean smtpTlsSsl) {
		this.mailSender = null;
		this.smtpTlsSsl = smtpTlsSsl;
	}

	public String getSmtpUsername() {
		return smtpUsername;
	}

	public void setSmtpUsername(String smtpUsername) {
		this.mailSender = null;
		this.smtpUsername = smtpUsername;
	}

	public char[] getSmtpPassword() {
		return encodedSmtpPassword.toCharArray();
	}

	public String getEncodedSmtpPassword() {
		return encodedSmtpPassword;
	}

	public void setSmtpPassword(String plainSmtpPassword) {
		this.encodedSmtpPassword = plainSmtpPassword;
	}

	public String getSmtpSender() {
		return smtpSender;
	}

	@Override
	public String getReplyToAddress() {
		if (this.replyToAddress == null) {
			if (this.customerEmail != null) {
				return customerEmail;
			}
			if (this.ownerEmail != null) {
				return ownerEmail;
			}
		}

		return replyToAddress;
	}

	public void setReplyToAddress(String replyToAddress) {
		this.replyToAddress = replyToAddress;
	}

	@Override
	public String[] getNotificationEmails() {
		return StringUtils.isBlank(notificationEmailList) ? ArrayUtils.EMPTY_STRING_ARRAY : StringUtils.split(notificationEmailList, ";");
	}

	public void setSmtpSender(String smtpSender) {
		this.smtpSender = smtpSender;
	}

	public String getNotificationEmailList() {
		return notificationEmailList;
	}

	public void setNotificationEmailList(String notificationEmailList) {
		this.notificationEmailList = notificationEmailList;
	}

	public ShopMailSender getMailSender(final SystemMailSenderConfig systemMailSenderConfig) {
		if (mailSender == null) {
			synchronized (this) {
				if (mailSender == null) {
					if (this.getUseOwnMailServer() && this.isConfigured()) {
						if (this.emailQueue == null) {
							setEmailQueue(systemMailSenderConfig.getEmailQueue());
						}
						mailSender = new ShopMailSender(this, this);
						log.info("Initialized private e-mail sender for shop {}", getId());
					} else if (systemMailSenderConfig != null && systemMailSenderConfig.isConfigured()) {
						mailSender = new ShopMailSender(this, new MailSenderConfig() {
							@Override
							public String getSmtpHost() {
								return systemMailSenderConfig.getSmtpHost();
							}

							@Override
							public boolean getSmtpTlsSsl() {
								return systemMailSenderConfig.getSmtpTlsSsl();
							}

							@Override
							public Integer getSmtpPort() {
								return systemMailSenderConfig.getSmtpPort();
							}

							@Override
							public String getSmtpUsername() {
								return systemMailSenderConfig.getSmtpUsername();
							}

							@Override
							public char[] getSmtpPassword() {
								return systemMailSenderConfig.getSmtpPassword();
							}

							@Override
							public String getSmtpSender() {
								return systemMailSenderConfig.getSmtpSender();
							}

							@Override
							public String[] getNotificationEmails() {
								return Shop.this.getNotificationEmails();
							}

							@Override
							public String getReplyToAddress() {
								return Shop.this.getReplyToAddress();
							}

							@Override
							public EmailQueue getEmailQueue() {
								return systemMailSenderConfig.getEmailQueue();
							}

							@Override
							public boolean isUsingSystemMailServer() {
								return systemMailSenderConfig.isUsingSystemMailServer();
							}
						});
						log.info("Using univocity e-mail server for shop {}", getId());
					}
				}
			}
		}
		return mailSender;
	}

	public void setEmailQueue(EmailQueue emailQueue) {
		this.emailQueue = emailQueue;
	}

	@Override
	public EmailQueue getEmailQueue() {
		return emailQueue;
	}

	public String getCustomerEmail() {
		return customerEmail;
	}

	public void setCustomerEmail(String customerEmail) {
		this.customerEmail = customerEmail;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getShopOwnerName() {
		return shopOwner;
	}

	public void setShopOwner(String shopOwner) {
		this.shopOwner = shopOwner;
	}

	public String getTimezone() {
		return timezone;
	}

	public void setTimezone(String timezone) {
		this.timezone = timezone;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getOwnerNameForEmailing() {
		return Utils.getName(getShopOwnerName(), null, getShopName() + " administrator");
	}

	public void setUseOwnMailServer(Boolean useOwnServer) {
		this.useOwnEmailServer = useOwnServer;
		mailSender = null;
	}

	public boolean getUseOwnMailServer() {
		return useOwnEmailServer != null && useOwnEmailServer;
	}

	public boolean isActive() {
		return active == null || active;
	}

	public boolean isInactive() {
		return !isActive();
	}

	public void setActive(Boolean active) {
		this.active = Boolean.valueOf(active);
	}

	public static String getAdminUrl(String shopName) {
		shopName = shopName.trim();
		if (shopName.endsWith(".myshopify.com")) {
			return "https://" + shopName + "/admin";
		} else {
			return "https://" + shopName + ".myshopify.com/admin";
		}
	}

	public static String getAdminUrl(Shop shop) {
		return getAdminUrl(shop.getShopName());
	}

	@Override
	public int compareTo(Shop o) {
		return this.getId().compareTo(o.getId());
	}

	@Override
	public boolean isUsingSystemMailServer() {
		return !this.getUseOwnMailServer();
	}

}
