CREATE TRIGGER variant_on_insert BEFORE INSERT ON variant FOR EACH ROW SET NEW.created_at = IFNULL(NEW.created_at, NOW()), NEW.updated_at = IFNULL(NEW.updated_at, NOW());
CREATE TRIGGER variant_on_update BEFORE UPDATE ON variant FOR EACH ROW SET NEW.updated_at = IFNULL(NEW.updated_at, NOW());

CREATE INDEX variant_shop_idx ON variant (shop_id, product_id, id) USING HASH;
