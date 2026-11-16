-- Retain provider images independently of the user's selected profile photo.
ALTER TABLE WEAVER_SOCIAL_ACCOUNT ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(2048) NULL;
-- Historical aggregate avatars cannot be attributed reliably to a provider.
-- Populate them on the next successful provider login/link instead.
