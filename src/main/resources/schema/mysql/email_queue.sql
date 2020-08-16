CREATE TRIGGER email_queue_insert BEFORE INSERT ON email_queue FOR EACH ROW SET NEW.created_at = IFNULL(NEW.created_at, NOW());

CREATE INDEX email_queue_idx ON email_queue (hash) USING HASH;