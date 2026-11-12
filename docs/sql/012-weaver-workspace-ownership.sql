-- Workspace IDs remain unchanged when ownership is transferred.
ALTER TABLE WEAVER_ACCOUNT_WORKSPACE ADD COLUMN IF NOT EXISTS owner_id CHAR(36) CHARACTER SET ascii NULL;
DELIMITER $$
CREATE OR REPLACE TRIGGER weaver_workspace_default_owner BEFORE INSERT ON WEAVER_ACCOUNT_WORKSPACE
FOR EACH ROW BEGIN
 IF NEW.owner_id IS NULL THEN SET NEW.owner_id=NEW.user_id; END IF;
END$$
DELIMITER ;
UPDATE WEAVER_ACCOUNT_WORKSPACE SET owner_id=user_id WHERE owner_id IS NULL;
-- Keep the nullable column plus trigger for rolling deployment with older INSERTs.
DELIMITER $$
DROP PROCEDURE IF EXISTS weaver_replace_workspace_fk$$
CREATE PROCEDURE weaver_replace_workspace_fk(IN source_table VARCHAR(64),IN source_column VARCHAR(64),IN target_table VARCHAR(64),IN target_column VARCHAR(64))
BEGIN
 DECLARE finished INT DEFAULT 0;
 DECLARE current_name VARCHAR(64);
 DECLARE current_table VARCHAR(64);
 DECLARE current_column VARCHAR(64);
 DECLARE correct INT DEFAULT 0;
 DECLARE refs CURSOR FOR SELECT CONSTRAINT_NAME,REFERENCED_TABLE_NAME,REFERENCED_COLUMN_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=source_table AND COLUMN_NAME=source_column AND REFERENCED_TABLE_NAME IS NOT NULL;
 DECLARE CONTINUE HANDLER FOR NOT FOUND SET finished=1;
 OPEN refs;
 scan_refs: LOOP
  FETCH refs INTO current_name,current_table,current_column;
  IF finished=1 THEN LEAVE scan_refs; END IF;
  IF current_table=target_table AND current_column=target_column THEN SET correct=1;
  ELSE
   SET @ddl=CONCAT('ALTER TABLE `',source_table,'` DROP FOREIGN KEY `',REPLACE(current_name,'`','``'),'`');
   PREPARE migration_stmt FROM @ddl;EXECUTE migration_stmt;DEALLOCATE PREPARE migration_stmt;
  END IF;
 END LOOP;
 CLOSE refs;
 IF target_table IS NOT NULL AND correct=0 THEN
  SET @ddl=CONCAT('ALTER TABLE `',source_table,'` ADD CONSTRAINT `fk_',LOWER(source_table),'_',source_column,'_owner12` FOREIGN KEY (`',source_column,'`) REFERENCES `',target_table,'`(`',target_column,'`) ON DELETE CASCADE');
  PREPARE migration_stmt FROM @ddl;EXECUTE migration_stmt;DEALLOCATE PREPARE migration_stmt;
 END IF;
END$$
DELIMITER ;
CALL weaver_replace_workspace_fk('WEAVER_ACCOUNT_WORKSPACE','user_id',NULL,NULL);
CALL weaver_replace_workspace_fk('WEAVER_ACCOUNT_WORKSPACE','owner_id','WEAVER_USER_ACCOUNT','id');
CALL weaver_replace_workspace_fk('WEAVER_WORKSPACE_ITEM','user_id','WEAVER_ACCOUNT_WORKSPACE','user_id');
CALL weaver_replace_workspace_fk('WEAVER_GUEST_IMPORT','user_id','WEAVER_ACCOUNT_WORKSPACE','user_id');
CALL weaver_replace_workspace_fk('WEAVER_WORKSPACE_MEDIA','user_id','WEAVER_ACCOUNT_WORKSPACE','user_id');
CALL weaver_replace_workspace_fk('WEAVER_WORKSPACE_MEMBER','workspace_id','WEAVER_ACCOUNT_WORKSPACE','user_id');
CALL weaver_replace_workspace_fk('WEAVER_WORKSPACE_INVITE','workspace_id','WEAVER_ACCOUNT_WORKSPACE','user_id');
CALL weaver_replace_workspace_fk('WEAVER_DOCUMENT_HISTORY','workspace_id','WEAVER_ACCOUNT_WORKSPACE','user_id');
CALL weaver_replace_workspace_fk('WEAVER_DOCUMENT_LOCK','workspace_id','WEAVER_ACCOUNT_WORKSPACE','user_id');
CALL weaver_replace_workspace_fk('WEAVER_ITEM_METADATA','workspace_id','WEAVER_ACCOUNT_WORKSPACE','user_id');
DROP PROCEDURE weaver_replace_workspace_fk;
