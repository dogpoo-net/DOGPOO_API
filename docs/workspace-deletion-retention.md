# Workspace deletion and restoration

Policy agreed 2026-11-18 (Asia/Seoul). No subscription gating is enabled yet.

- A newly created workspace with no saved items, media, invitations, members,
  document history or subsequent revision changes can be permanently deleted by
  its owner without email verification. Existing delete confirmation remains.
- Any work history requires a dedicated deletion code sent to the current owner's
  account email. Emptying a workspace does not reset its revision/history.
- Codes expire after 10 minutes, allow five verification attempts and have a
  60-second resend cooldown plus an account rate limit. The hash is bound to the
  workspace, owner, current email and purpose. Codes are not logged or stored raw.
- Eligibility and ownership are rechecked under the workspace lock at deletion.
  A blank preview does not authorize deleting data subsequently written by someone.
- Verified deletion archives the workspace for 60 days. It disappears from active
  catalogs; normal reads, writes and invitation acceptance are denied. Memberships,
  authorship, history and attachments remain available for owner restoration.
- `deleted-workspaces` lists only the owner's unexpired archives. `restore-workspace`
  preserves original IDs/content and rejects expired retention periods.
- A once-per-minute job rechecks deadlines under a row lock and permanently deletes
  expired DB data, enqueuing physical attachment erasure through the existing
  durable deletion queue. File deletion failures are retried. This is a recovery
  retention period, not a database backup policy.
- Deleted IDs retain a minimal tombstone so legacy initialization cannot resurrect
  them. Account withdrawal remains a separate erasure operation and includes owned
  archives in its confirmation inventory and attachment cleanup.

Apply `021-weaver-workspace-retention.sql` before deploying the API. The migration
adds archive timestamps and the purpose-specific challenge table. The web picker
provides email verification, the archive/restore list, and per-workspace member
management without switching the active workspace.

Follow-up product decision: which plans include restoration and any retention or
storage allowances. The current implementation offers restoration to every owner.
