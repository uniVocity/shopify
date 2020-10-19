CREATE TRIGGER image_on_insert BEFORE INSERT ON image FOR EACH ROW SET NEW.created_at = IFNULL(NEW.created_at, NOW()), NEW.updated_at = IFNULL(NEW.updated_at, NOW());
CREATE TRIGGER image_on_update BEFORE UPDATE ON image FOR EACH ROW SET NEW.updated_at = IFNULL(NEW.updated_at, NOW());

CREATE INDEX image_shop_idx ON image (shop_id, id) USING HASH;