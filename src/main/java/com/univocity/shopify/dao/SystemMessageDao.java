

package com.univocity.shopify.dao;

import com.univocity.shopify.dao.base.*;
import com.univocity.shopify.exception.*;
import com.univocity.shopify.model.db.*;
import org.slf4j.*;

public class SystemMessageDao extends EntityDao<SystemMessage> {
	private static final Logger log = LoggerFactory.getLogger(SystemMessage.class);

	private static final String SELECT_TEMPLATE_BY_TYPE = "SELECT * FROM system_message WHERE type = ? AND deleted_at IS NULL";

	public SystemMessageDao() {
		super("system_message");

		enableCacheFor(SELECT_TEMPLATE_BY_TYPE, template -> new Object[]{template.getMessageType().code});
	}

	public void init() {
		long count = db.count("SELECT COUNT(*) FROM system_message");
		if (count == 0) {
			log.info("No system_messages defined in system_message table. Initializing with defaults");

			for(MessageType messageType : MessageType.values()){
				SystemMessage message = messageType.getDefaultMessage();
				persist(message);
			}
		}
	}

	public SystemMessage getMessageTemplate(MessageType messageType) {
		SystemMessage out = queryForOptionalEntity(SELECT_TEMPLATE_BY_TYPE, messageType.code);
		if(out == null){
			log.info("No message defined in system_message table for type {}. Initializing with defaults", messageType);
			out = messageType.getDefaultMessage();
			persist(out);
		}
		return out;
	}

	@Override
	protected SystemMessage newEntity() {
		return new SystemMessage();
	}

	@Override
	protected String[] identifierNames() {
		return new String[]{"id"};
	}

	@Override
	protected Object[] identifierValues(SystemMessage entity) {
		return new Object[]{entity.getId()};
	}

	@Override
	protected void validate(Operation operation, SystemMessage entity) {
		if (entity.getMessageType() == null) {
			throw new ValidationException("System message type cannot be null");
		}
	}
}
