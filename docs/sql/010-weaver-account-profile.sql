-- Account-owned profile, independent of workspace membership.
CREATE TABLE IF NOT EXISTS WEAVER_ACCOUNT_PROFILE (
 user_id CHAR(36) CHARACTER SET ascii PRIMARY KEY,
 avatar_url VARCHAR(2048) NULL,
 avatar_path VARCHAR(180) NULL,
 email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
 FOREIGN KEY(user_id) REFERENCES WEAVER_USER_ACCOUNT(id) ON DELETE CASCADE
) ENGINE=InnoDB;
-- Legacy social credentials cannot be distinguished from linked email credentials.
-- A successful password login restores email_enabled for those accounts.
INSERT IGNORE INTO WEAVER_ACCOUNT_PROFILE(user_id,email_enabled)
 SELECT a.id,NOT EXISTS(SELECT 1 FROM WEAVER_SOCIAL_ACCOUNT s WHERE s.user_id=a.id) FROM WEAVER_USER_ACCOUNT a;
CREATE TABLE IF NOT EXISTS WEAVER_PASSWORD_CHALLENGE (
 user_id CHAR(36) CHARACTER SET ascii PRIMARY KEY,
 token_hash CHAR(64) CHARACTER SET ascii NOT NULL,
 expires_at DATETIME(6) NOT NULL,
 sent_at DATETIME(6) NOT NULL,
 attempts INT NOT NULL DEFAULT 0,
 FOREIGN KEY(user_id) REFERENCES WEAVER_USER_ACCOUNT(id) ON DELETE CASCADE
) ENGINE=InnoDB;

ALTER TABLE WEAVER_ACCOUNT_PROFILE ADD COLUMN IF NOT EXISTS default_face TINYINT NULL;
UPDATE WEAVER_ACCOUNT_PROFILE SET default_face=FLOOR(RAND()*6) WHERE default_face IS NULL;
