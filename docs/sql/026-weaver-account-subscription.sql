SET SESSION lock_wait_timeout=15;
CREATE TABLE IF NOT EXISTS WEAVER_ACCOUNT_SUBSCRIPTION (
 user_id CHAR(36) CHARACTER SET ascii PRIMARY KEY,
 plan VARCHAR(16) CHARACTER SET ascii NOT NULL,
 period_end DATETIME(6) NOT NULL,
 cancel_at_end BOOLEAN NOT NULL DEFAULT FALSE,
 updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 FOREIGN KEY(user_id) REFERENCES WEAVER_USER_ACCOUNT(id) ON DELETE CASCADE,
 FOREIGN KEY(plan) REFERENCES WEAVER_PLAN_POLICY(code)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS WEAVER_POLICY_MIGRATION (
 id VARCHAR(64) CHARACTER SET ascii PRIMARY KEY,
 applied_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;
START TRANSACTION;
-- Preserve the strongest active workspace grant, then the latest expiry of that tier.
-- Never combine a higher tier with another grant's later expiration.
INSERT IGNORE INTO WEAVER_ACCOUNT_SUBSCRIPTION(user_id,plan,period_end,cancel_at_end,updated_at)
SELECT owner_id,plan,period_end,cancel_at_end,updated_at FROM (
 SELECT w.owner_id,s.plan,s.period_end,s.cancel_at_end,s.updated_at,
 ROW_NUMBER() OVER(PARTITION BY w.owner_id ORDER BY FIELD(s.plan,'pro','standard','free'),s.period_end DESC,s.workspace_id) AS rn
 FROM WEAVER_WORKSPACE_SUBSCRIPTION s JOIN WEAVER_ACCOUNT_WORKSPACE w ON w.user_id=s.workspace_id
 WHERE s.period_end>UTC_TIMESTAMP(6) AND s.plan IN ('standard','pro')
) ranked WHERE rn=1 AND NOT EXISTS(SELECT 1 FROM WEAVER_POLICY_MIGRATION WHERE id='account-subscription-026');
INSERT IGNORE INTO WEAVER_POLICY_MIGRATION(id) VALUES('account-subscription-026');
COMMIT;
CREATE TABLE IF NOT EXISTS WEAVER_ACCOUNT_CREATION (
 user_id CHAR(36) CHARACTER SET ascii PRIMARY KEY,
 last_created_at DATETIME(6) NOT NULL,
 FOREIGN KEY(user_id) REFERENCES WEAVER_USER_ACCOUNT(id) ON DELETE CASCADE
) ENGINE=InnoDB;
-- Legacy creation timestamps were not recorded. Do not fabricate a cooldown for them.
UPDATE WEAVER_PLAN_POLICY SET owned_limit=2 WHERE code='free';
UPDATE WEAVER_PLAN_POLICY SET owned_limit=10 WHERE code='standard';
UPDATE WEAVER_PLAN_POLICY SET owned_limit=NULL WHERE code='pro';
