package com.univocity.shopify.service;

import com.univocity.shopify.dao.*;
import com.univocity.shopify.email.*;
import com.univocity.shopify.model.db.*;
import com.univocity.shopify.price.*;
import com.univocity.shopify.utils.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;

import java.math.*;
import java.time.*;

import static java.time.temporal.ChronoUnit.*;


public class OrderProcessingService {

	private static final Logger log = LoggerFactory.getLogger(OrderProcessingService.class);

	private static final SimpleCache<Product> transientProducts = new SimpleCache<>(10000);

	@Autowired
	ShopDao shops;

	@Autowired
	App app;

	@Autowired
	OrdersDao orders;

	@Autowired
	CustomerDao customers;

	@Autowired
	EmailQueue emailQueue;

	@Autowired
	EmailTemplateDao emails;

	@Autowired
	PriceAggregator priceAggregator;

	@Autowired
	SystemMailSenderConfig systemMailSenderConfig;

	private String getCustomerName(Order order, Shop shop) {
		String customerName = "customer";
		if (order.getCustomer() == null && order.getCustomerId() != null) {
			Customer customer = customers.getOptionalEntityById(shop.getId(), order.getCustomerId());
			customerName = customer.getName();
		}
		return customerName;
	}

	private void calculateAmountPayable(Order order, Shop shop) {
		//TODO: can't do much here for now. Only ADA exists on the cardano blockchain
//		order.setCryptoTokenSymbol("ADA");

		//TODO: generate payment address on the fly from shop public root key
		order.setPaymentAddress("addr1q9jap0f549256uvkmrwv2yq9vruphxqrm92szg03xgx4dhtdd0sqyslnjxvce9syyw4ktnrh0n7ct60zrs29wnef3jqqttmm2g");

		//TODO: priceAggregator here must take crypto asset symbol and return its price. It's hardcoded for ADA right now.
		BigDecimal unit = BigDecimal.valueOf(priceAggregator.getPrice());

		BigDecimal totalPriceCrypto = order.getTotalPriceUsd().divide(unit, 6, RoundingMode.HALF_UP); //TODO: SCALE OF 6 WORKS FOR ADA but it has to be token specific
		order.setTotalPriceCrypto(totalPriceCrypto);

		//TODO: allow for discounts in crypto payments.

		//TODO: update status on shopify order too
		order.setStatus(FinancialStatus.authorized);

		//TODO: update only required fields instead of everything.
		orders.persist(order);
	}

	public void processNewOrder(Order order, Shop shop) {
		String customerName = getCustomerName(order, shop);
		calculateAmountPayable(order, shop);

		EmailTemplate template = emails.getEmailTemplate(shop.getId(), EmailType.ORDER_PLACED);
		MessageRenderer renderer = template.renderer();

		renderer.setStoreName(shop);
		renderer.setName(customerName);
		renderer.setOrderUrl(order.getStatusUrl(shop.getDomain()));
		renderer.setOrderId(String.valueOf(order.getShopifyOrderNumber()));
		renderer.setCheckoutUrl(order.getStatusUrl(shop.getDomain()));

		//TODO: must be configurable by store owner. Also
		renderer.setExpirationDate(new java.sql.Date(Instant.now().plus(1, HOURS).toEpochMilli()));

		//TODO: update later when more crypto currencies become available.
		renderer.setCryptoTokenSymbol("ADA"/*order.getCryptoTokenSymbol()*/);
		renderer.setPaymentAddress(order.getPaymentAddress());


		//TODO: will have to format price here to conform to the correct number of decimal symbols used by the crypto token.
		renderer.setOrderAmount(order.getTotalPriceCrypto().toPlainString());

		String title = renderer.renderTitle();
		String body = renderer.renderBody();

		//TODO: for now send e-mail to customer and shop owner, we're only testing for now.
		Email email = shop.getMailSender(systemMailSenderConfig).newEmail(shop.getId(), new String[]{order.getEmail(), shop.getOwnerEmail()}, title, body);
		email.setReplyTo(shop.getReplyToAddress());

		log.info("Sending order payment e-mail to customer {} of shop {}. Order ID: {}, Order amount: {} {}", customerName, shop.getShopName(), order.getId(), order.getTotalPriceCrypto(), "ADA"/*order.getCryptoTokenSymbol() FIXME */);

		emailQueue.offer(email);
	}
}
