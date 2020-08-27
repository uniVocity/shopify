CREATE TRIGGER line_item_on_insert BEFORE INSERT ON line_item FOR EACH ROW SET NEW.created_at = IFNULL(NEW.created_at, NOW()), NEW.updated_at = IFNULL(NEW.updated_at, NOW());
CREATE TRIGGER line_item_on_update BEFORE UPDATE ON line_item FOR EACH ROW SET NEW.updated_at = IFNULL(NEW.updated_at, NOW());
