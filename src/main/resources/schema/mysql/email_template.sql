CREATE TRIGGER email_template_on_insert BEFORE INSERT ON email_template FOR EACH ROW SET NEW.created_at = IFNULL(NEW.created_at, NOW()), NEW.updated_at = IFNULL(NEW.updated_at, NOW());
CREATE TRIGGER email_template_on_update BEFORE UPDATE ON email_template FOR EACH ROW SET NEW.updated_at = IFNULL(NEW.updated_at, NOW());

CREATE INDEX email_template_idx ON email_template (shop_id, id) USING HASH;