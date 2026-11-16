-- Serialize account creation/link/unlink so cross-table email ownership checks
-- cannot race. No workspace content is changed.
CREATE TABLE IF NOT EXISTS WEAVER_AUTH_WRITE_LOCK (id TINYINT PRIMARY KEY) ENGINE=InnoDB;
INSERT IGNORE INTO WEAVER_AUTH_WRITE_LOCK VALUES(1);
ALTER TABLE WEAVER_SOCIAL_ACCOUNT ADD COLUMN IF NOT EXISTS login_email VARCHAR(254) CHARACTER SET ascii COLLATE ascii_general_ci NULL;
ALTER TABLE WEAVER_SOCIAL_ACCOUNT ADD INDEX IF NOT EXISTS idx_social_email(login_email);
ALTER TABLE WEAVER_USER_ACCOUNT ADD COLUMN IF NOT EXISTS signup_provider VARCHAR(16) CHARACTER SET ascii NULL;
UPDATE WEAVER_USER_ACCOUNT a LEFT JOIN WEAVER_ACCOUNT_PROFILE p ON p.user_id=a.id
SET a.signup_provider=CASE WHEN COALESCE(p.email_enabled,TRUE) THEN 'email'
 ELSE COALESCE((SELECT s.provider FROM WEAVER_SOCIAL_ACCOUNT s WHERE s.user_id=a.id ORDER BY s.created_at,s.provider LIMIT 1),'unknown') END
WHERE a.signup_provider IS NULL;
-- Only the signup provider's email is recoverable from the old schema.
UPDATE WEAVER_SOCIAL_ACCOUNT s JOIN WEAVER_USER_ACCOUNT a ON a.id=s.user_id AND a.signup_provider=s.provider
SET s.login_email=a.email WHERE s.login_email IS NULL;
