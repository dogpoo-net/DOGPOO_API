# Authenticated workspace storage and collaboration

Apply `sql/007-weaver-owned-workspaces.sql` and
`sql/008-weaver-sharing-history.sql` before deploying this version. Both migrations
are additive and retain legacy backups. Tables follow the `WEAVER_` prefix rule.

All workspace endpoints live under
`/weaver/api/auth/session/{web|native}/workspace` (with the application's existing
`/dogpoo` servlet prefix). Web calls require the configured Origin, tab proof and
HttpOnly cookie; native calls require an access token. Identity and history actors
come from the verified session. `workspaceId` defaults to the user's own UUID;
joined workspaces use the owner's UUID. Never use client-supplied email as identity.

- `read`, `save`: incremental item revisions and optimistic conflict detection.
  Saves acknowledge only submitted items. Layout positions are separate items.
- `claim`: an idempotent, globally unique guest-source claim. Retry does not
  overwrite later edits; another owner cannot reuse the same claim.
- `media/write`, `media/read`: account/workspace-authorized attachment bytes.
  `WEAVER_RESOURCE_DIR` defaults to `/home/ocaptain/resource`; files live under
  `image/<workspace UUID>` or `audio/<workspace UUID>`. Metadata and SHA-256 are in DB.
- `catalog`, `members`: personal/joined workspaces, received invitations and roles.
- `invite`, `accept`, `revoke`, `remove-member`, `member-role`: owner-managed
  collaboration. Invitations require the verified recipient email, expire in seven
  days, and store only token hashes. Sending uses existing SMTP configuration.
- `history`: newest 50 document body versions, paginated with `beforeVersion`.
  Diffs/counts use Unicode code points, including whitespace and markup. History
  starts when existing data is migrated or a document is saved/imported.

Free workspace limit: two editors including the owner. Pending editor invitations
reserve a seat. Viewers do not consume seats and cannot write items or media.
Membership changes, capacity checks and saves serialize on the workspace row.
Joining a workspace keeps the user's personal workspace available.

`WEAVER_WORKSPACE_MAX_BYTES` defaults to 67108864, covering items, media and
history. Billing/tier management is not implemented. Historical restore and live
collaborative editing are not included. Same-item stale saves return 409 and the
client retains unsynced local data. Legacy unauthenticated backup APIs return 410.

Verification: `mvn verify`; actual database tests are opt-in through
`WEAVER_AUTH_DB_TEST=true`. Integration tests create isolated UUID accounts with
`@example.invalid` emails, do not send mail, and delete their DB fixtures afterward.

Existing document editors obtain an exclusive 120-second lease, renewed every
30 seconds and released on close. Another editor sees read-only content. Losing
the lease blocks saves without discarding the current input. Forced tab closure
recovers after lease expiry. Lease acquisition is authorized and serialized with
saves; actor plus per-editor token must match. Optimistic revisions still protect
stale content after a lease is transferred. Lease renewal does not save content.
