CREATE TRIGGER system_message_on_insert BEFORE INSERT ON system_message FOR EACH ROW SET NEW.created_at = IFNULL(NEW.created_at, NOW()), NEW.updated_at = IFNULL(NEW.updated_at, NOW());
CREATE TRIGGER system_message_on_update BEFORE UPDATE ON system_message FOR EACH ROW SET NEW.updated_at = IFNULL(NEW.updated_at, NOW());
