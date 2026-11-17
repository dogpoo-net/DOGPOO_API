-- Stop API writers for the migration. DDL is idempotent; content moves are transactional.
-- Retain the nullable legacy payload column, empty, for explicit rollback tooling.
ALTER TABLE WEAVER_WORKSPACE_ITEM ADD COLUMN IF NOT EXISTS payload_bytes BIGINT NOT NULL DEFAULT 0;
CREATE TABLE IF NOT EXISTS WEAVER_MEMO_CONTENT (
 workspace_id CHAR(36) CHARACTER SET ascii NOT NULL,
 item_key CHAR(64) CHARACTER SET ascii NOT NULL,
 payload LONGTEXT NOT NULL,
 PRIMARY KEY(workspace_id,item_key),
 FOREIGN KEY(workspace_id,item_key) REFERENCES WEAVER_WORKSPACE_ITEM(user_id,item_key) ON DELETE CASCADE
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS WEAVER_DOCUMENT_CONTENT (
 workspace_id CHAR(36) CHARACTER SET ascii NOT NULL,
 item_key CHAR(64) CHARACTER SET ascii NOT NULL,
 payload LONGTEXT NOT NULL,
 PRIMARY KEY(workspace_id,item_key),
 FOREIGN KEY(workspace_id,item_key) REFERENCES WEAVER_WORKSPACE_ITEM(user_id,item_key) ON DELETE CASCADE
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS WEAVER_CANVAS_LAYOUT (
 workspace_id CHAR(36) CHARACTER SET ascii NOT NULL,
 item_key CHAR(64) CHARACTER SET ascii NOT NULL,
 payload LONGTEXT NOT NULL,
 PRIMARY KEY(workspace_id,item_key),
 FOREIGN KEY(workspace_id,item_key) REFERENCES WEAVER_WORKSPACE_ITEM(user_id,item_key) ON DELETE CASCADE
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS WEAVER_WORKSPACE_STRUCTURE (
 workspace_id CHAR(36) CHARACTER SET ascii NOT NULL,
 item_key CHAR(64) CHARACTER SET ascii NOT NULL,
 payload LONGTEXT NOT NULL,
 PRIMARY KEY(workspace_id,item_key),
 FOREIGN KEY(workspace_id,item_key) REFERENCES WEAVER_WORKSPACE_ITEM(user_id,item_key) ON DELETE CASCADE
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS WEAVER_WORKSPACE_SETTING (
 workspace_id CHAR(36) CHARACTER SET ascii NOT NULL,
 item_key CHAR(64) CHARACTER SET ascii NOT NULL,
 payload LONGTEXT NOT NULL,
 PRIMARY KEY(workspace_id,item_key),
 FOREIGN KEY(workspace_id,item_key) REFERENCES WEAVER_WORKSPACE_ITEM(user_id,item_key) ON DELETE CASCADE
) ENGINE=InnoDB;
DELIMITER $$
DROP PROCEDURE IF EXISTS weaver_split_item_content$$
CREATE PROCEDURE weaver_split_item_content()
BEGIN
 DECLARE broken BIGINT DEFAULT 0;
 DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
 START TRANSACTION;
 INSERT INTO WEAVER_MEMO_CONTENT(workspace_id,item_key,payload) SELECT user_id,item_key,payload FROM WEAVER_WORKSPACE_ITEM WHERE deleted=FALSE AND payload IS NOT NULL AND (kind IN ('list:elements','list:trash')) ON DUPLICATE KEY UPDATE payload=VALUES(payload);
 INSERT INTO WEAVER_DOCUMENT_CONTENT(workspace_id,item_key,payload) SELECT user_id,item_key,payload FROM WEAVER_WORKSPACE_ITEM WHERE deleted=FALSE AND payload IS NOT NULL AND (kind='list:notes') ON DUPLICATE KEY UPDATE payload=VALUES(payload);
 INSERT INTO WEAVER_CANVAS_LAYOUT(workspace_id,item_key,payload) SELECT user_id,item_key,payload FROM WEAVER_WORKSPACE_ITEM WHERE deleted=FALSE AND payload IS NOT NULL AND (kind='layout') ON DUPLICATE KEY UPDATE payload=VALUES(payload);
 INSERT INTO WEAVER_WORKSPACE_STRUCTURE(workspace_id,item_key,payload) SELECT user_id,item_key,payload FROM WEAVER_WORKSPACE_ITEM WHERE deleted=FALSE AND payload IS NOT NULL AND (kind IN ('list:memoBoxes','list:memoBoxFolders','list:folders','list:elementFolders','list:hashtagFolders','list:paragraphFolders','map:memoBoxFolderParents','map:hashtagFolderAssignments')) ON DUPLICATE KEY UPDATE payload=VALUES(payload);
 INSERT INTO WEAVER_WORKSPACE_SETTING(workspace_id,item_key,payload) SELECT user_id,item_key,payload FROM WEAVER_WORKSPACE_ITEM WHERE deleted=FALSE AND payload IS NOT NULL AND (NOT (kind IN ('list:elements','list:trash') OR kind='list:notes' OR kind='layout' OR kind IN ('list:memoBoxes','list:memoBoxFolders','list:folders','list:elementFolders','list:hashtagFolders','list:paragraphFolders','map:memoBoxFolderParents','map:hashtagFolderAssignments'))) ON DUPLICATE KEY UPDATE payload=VALUES(payload);
 DELETE c FROM WEAVER_MEMO_CONTENT c JOIN WEAVER_WORKSPACE_ITEM w ON w.user_id=c.workspace_id AND w.item_key=c.item_key WHERE w.deleted=TRUE;
 DELETE c FROM WEAVER_DOCUMENT_CONTENT c JOIN WEAVER_WORKSPACE_ITEM w ON w.user_id=c.workspace_id AND w.item_key=c.item_key WHERE w.deleted=TRUE;
 DELETE c FROM WEAVER_CANVAS_LAYOUT c JOIN WEAVER_WORKSPACE_ITEM w ON w.user_id=c.workspace_id AND w.item_key=c.item_key WHERE w.deleted=TRUE;
 DELETE c FROM WEAVER_WORKSPACE_STRUCTURE c JOIN WEAVER_WORKSPACE_ITEM w ON w.user_id=c.workspace_id AND w.item_key=c.item_key WHERE w.deleted=TRUE;
 DELETE c FROM WEAVER_WORKSPACE_SETTING c JOIN WEAVER_WORKSPACE_ITEM w ON w.user_id=c.workspace_id AND w.item_key=c.item_key WHERE w.deleted=TRUE;
 SELECT COUNT(*) INTO broken FROM WEAVER_WORKSPACE_ITEM w LEFT JOIN WEAVER_MEMO_CONTENT c0 ON c0.workspace_id=w.user_id AND c0.item_key=w.item_key LEFT JOIN WEAVER_DOCUMENT_CONTENT c1 ON c1.workspace_id=w.user_id AND c1.item_key=w.item_key LEFT JOIN WEAVER_CANVAS_LAYOUT c2 ON c2.workspace_id=w.user_id AND c2.item_key=w.item_key LEFT JOIN WEAVER_WORKSPACE_STRUCTURE c3 ON c3.workspace_id=w.user_id AND c3.item_key=w.item_key LEFT JOIN WEAVER_WORKSPACE_SETTING c4 ON c4.workspace_id=w.user_id AND c4.item_key=w.item_key
 WHERE w.deleted=FALSE AND (COALESCE(c0.payload,c1.payload,c2.payload,c3.payload,c4.payload) IS NULL OR (w.payload IS NOT NULL AND NOT (BINARY w.payload <=> BINARY COALESCE(c0.payload,c1.payload,c2.payload,c3.payload,c4.payload))));
 IF broken>0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Workspace content verification failed; original payloads preserved'; END IF;
 UPDATE WEAVER_WORKSPACE_ITEM w LEFT JOIN WEAVER_MEMO_CONTENT c0 ON c0.workspace_id=w.user_id AND c0.item_key=w.item_key LEFT JOIN WEAVER_DOCUMENT_CONTENT c1 ON c1.workspace_id=w.user_id AND c1.item_key=w.item_key LEFT JOIN WEAVER_CANVAS_LAYOUT c2 ON c2.workspace_id=w.user_id AND c2.item_key=w.item_key LEFT JOIN WEAVER_WORKSPACE_STRUCTURE c3 ON c3.workspace_id=w.user_id AND c3.item_key=w.item_key LEFT JOIN WEAVER_WORKSPACE_SETTING c4 ON c4.workspace_id=w.user_id AND c4.item_key=w.item_key SET w.payload_bytes=IF(w.deleted,0,OCTET_LENGTH(COALESCE(c0.payload,c1.payload,c2.payload,c3.payload,c4.payload))),w.payload=NULL;
 COMMIT;
END$$
DELIMITER ;
CALL weaver_split_item_content();
DROP PROCEDURE weaver_split_item_content;
