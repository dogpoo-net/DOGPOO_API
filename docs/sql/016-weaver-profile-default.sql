-- Keep an explicitly selected default image when additional social accounts are linked.
ALTER TABLE WEAVER_ACCOUNT_PROFILE ADD COLUMN IF NOT EXISTS default_selected BOOLEAN NOT NULL DEFAULT FALSE;
