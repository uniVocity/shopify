CREATE TRIGGER product_on_insert BEFORE INSERT ON product FOR EACH ROW SET NEW.created_at = IFNULL(NEW.created_at, NOW()), NEW.updated_at = IFNULL(NEW.updated_at, NOW());
CREATE TRIGGER product_on_update BEFORE UPDATE ON product FOR EACH ROW SET NEW.updated_at = IFNULL(NEW.updated_at, NOW());

CREATE INDEX product_shop_idx ON product (shop_id, id) USING HASH;
CREATE INDEX product_name_idx ON product (shop_id, name) USING HASH;