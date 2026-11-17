-- Use the original archive time, not migration time, for existing recoverable spaces.
UPDATE WEAVER_ACCOUNT_WORKSPACE
SET purge_after=DATE_ADD(archived_at, INTERVAL 30 DAY)
WHERE archived_at IS NOT NULL;
