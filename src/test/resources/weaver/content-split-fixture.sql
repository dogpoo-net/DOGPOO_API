-- Isolated test database only: minimal parents plus representative legacy content.
CREATE TABLE WEAVER_ACCOUNT_WORKSPACE(user_id CHAR(36) CHARACTER SET ascii PRIMARY KEY,revision BIGINT DEFAULT 0) ENGINE=InnoDB;
CREATE TABLE WEAVER_USER_ACCOUNT(id CHAR(36) CHARACTER SET ascii PRIMARY KEY,display_name VARCHAR(100));
CREATE TABLE WEAVER_ITEM_METADATA(workspace_id CHAR(36) CHARACTER SET ascii,item_key CHAR(64) CHARACTER SET ascii,created_by CHAR(36) CHARACTER SET ascii,updated_at DATETIME(6),PRIMARY KEY(workspace_id,item_key));
CREATE TABLE WEAVER_WORKSPACE_ITEM(user_id CHAR(36) CHARACTER SET ascii NOT NULL,item_key CHAR(64) CHARACTER SET ascii NOT NULL,kind VARCHAR(64) NOT NULL,item_id VARCHAR(512) NOT NULL,payload LONGTEXT,deleted BOOLEAN NOT NULL DEFAULT FALSE,revision BIGINT NOT NULL,PRIMARY KEY(user_id,item_key),KEY(user_id,revision),FOREIGN KEY(user_id) REFERENCES WEAVER_ACCOUNT_WORKSPACE(user_id) ON DELETE CASCADE) ENGINE=InnoDB;
INSERT INTO WEAVER_ACCOUNT_WORKSPACE VALUES('legacy',10);
INSERT INTO WEAVER_WORKSPACE_ITEM VALUES
('legacy','a','list:elements','a','{"text":"한글 메모 😀"}',FALSE,1),
('legacy','b','list:trash','b','{"text":"휴지통"}',FALSE,2),
('legacy','c','list:notes','c','{"project":{"document":"본문"}}',FALSE,3),
('legacy','d','layout','d','12.5',FALSE,4),
('legacy','e','list:memoBoxes','e','{"name":"상자"}',FALSE,5),
('legacy','f','map:colors','f','123',FALSE,6),
('legacy','g','list:elements','g',NULL,TRUE,7),
('legacy','h','map:futureSetting','h','false',FALSE,8);
