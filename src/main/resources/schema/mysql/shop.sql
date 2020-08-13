CREATE TRIGGER shop_on_insert BEFORE INSERT ON shop FOR EACH ROW SET NEW.created_at = IFNULL(NEW.created_at, NOW()), NEW.updated_at = IFNULL(NEW.updated_at, NOW());
CREATE TRIGGER shop_on_update BEFORE UPDATE ON shop FOR EACH ROW SET NEW.updated_at = IFNULL(NEW.updated_at, NOW());


