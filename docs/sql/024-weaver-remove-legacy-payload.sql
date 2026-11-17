-- Apply only after 023 and deploying an API that does not write the legacy column.
-- Refuse removal if any legacy data remains. Bound metadata lock waits.
SET SESSION lock_wait_timeout=15;
DELIMITER $$
DROP PROCEDURE IF EXISTS weaver_drop_legacy_payload$$
CREATE PROCEDURE weaver_drop_legacy_payload()
BEGIN
 IF EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='WEAVER_WORKSPACE_ITEM' AND COLUMN_NAME='payload') THEN
  IF EXISTS(SELECT 1 FROM WEAVER_WORKSPACE_ITEM WHERE payload IS NOT NULL) THEN
   SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Legacy payload data remains; complete migration 023 first';
  END IF;
  ALTER TABLE WEAVER_WORKSPACE_ITEM DROP COLUMN payload;
 END IF;
END$$
DELIMITER ;
CALL weaver_drop_legacy_payload();
DROP PROCEDURE weaver_drop_legacy_payload;
