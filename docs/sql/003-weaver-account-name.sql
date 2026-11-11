-- Existing accounts retain NULL until they supply a name; identifiers never change.
ALTER TABLE WEAVER_USER_ACCOUNT
 ADD COLUMN IF NOT EXISTS display_name VARCHAR(100) CHARACTER SET utf8mb4 NULL AFTER email;
