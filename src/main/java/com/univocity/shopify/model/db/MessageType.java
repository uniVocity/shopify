

package com.univocity.shopify.model.db;

public enum MessageType {

	APP_INSTALLED(1),
	APP_UNINSTALLED(2),
	DATA_REQUEST(3);

	public final int code;

	MessageType(int type) {
		this.code = type;
	}

	public static MessageType fromCode(int code) {
		for (MessageType t : MessageType.values()) {
			if (t.code == code) {
				return t;
			}
		}
		throw new IllegalArgumentException("Unknown message type code: '" + code + "'");
	}

	public SystemMessage getDefaultMessage() {
		SystemMessage message;
		switch (this) {
			case APP_UNINSTALLED:
				message = new SystemMessage();
				message.setMessageType(MessageType.APP_UNINSTALLED);
				message.setTitle("Thank you for using our app.");
				message.setBody("" +
						"We'd appreciate if you could reply to this e-mail and leave us any sort of feedback about your" +
						" experience with our app and any suggestions of features you want or issues you had." +
						"\r\n" +
						"\r\nWe usually reply in less than 24 hours." +
						"\r\n" +
						"\r\nThank your support." +
						"\r\nThe univocity team");
				return message;
			case DATA_REQUEST:
				message = new SystemMessage();
				message.setMessageType(MessageType.DATA_REQUEST);
				message.setTitle("Data request details.");
				message.setBody("You recently requested what data is stored in the cardano integration for Shopify. The requested data is as follows:" +
						"\r\n\r\n{{ TEXT }}" +
						"\r\n\r\nContact us if you have any concern. Just reply to this e-mail and we'll be happy to assist." +
						"\r\nThank you for using our app." +
						"\r\nThe univocity team");
				return message;
			case APP_INSTALLED:
				message = new SystemMessage();
				message.setMessageType(MessageType.APP_INSTALLED);
				message.setTitle("Welcome to the cardano integration for Shopify.");
				message.setBody("" +
						"If you need anything just send us an e-mail and we'll be happy to assist." +
						"\r\n" +
						"\r\nFor Feature requests and bug reports just reply to this e-mail or open a new issue on github:" +
						"\r\n" +
						"\r\n * https://github.com/univocity/shopify/issues" +
						"\r\n" +
						"\r\nWe usually reply in less than 24 hours." +
						"\r\n" +
						"\r\nThank you for installing our app." +
						"\r\nThe univocity team");
				return message;
			default:
				return null;
		}
	}
}