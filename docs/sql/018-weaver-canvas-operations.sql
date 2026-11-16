-- Durable idempotency receipts: retained with the workspace so an offline retry
-- cannot replay an already committed move over a later collaborator's move.
CREATE TABLE IF NOT EXISTS WEAVER_CANVAS_OPERATION_RECEIPT (
 workspace_id CHAR(36) CHARACTER SET ascii NOT NULL,
 operation_id CHAR(64) CHARACTER SET ascii NOT NULL,
 request_hash CHAR(64) CHARACTER SET ascii NOT NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 PRIMARY KEY(workspace_id,operation_id),
 CONSTRAINT fk_weaver_canvas_workspace FOREIGN KEY(workspace_id)
 REFERENCES WEAVER_ACCOUNT_WORKSPACE(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;
