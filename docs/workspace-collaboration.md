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

Migration 009 adds WEAVER_ITEM_METADATA for authenticated creator and last-update
identity/time, returned alongside item payloads. Existing clients may ignore these
optional response fields. Existing content without creator metadata stays unknown;
editing such content never invents a creator. Another editor's save updates the
modification time while preserving the original creator.

Document revisions saved while the server document lock has `shared_editing=TRUE`
now store `action=unlocked_edit`. This is decided by the server under the workspace
transaction lock, not by a client assertion. It applies to every editor saving
that unlocked document, and does not revoke the original editor's access.
Existing revisions retain their original actions; no historical unlock status is
inferred. The existing history `action` column accommodates this value, so no
schema migration is required.

Document editors can observe a lease with `observeOnly=true` without acquiring
one. The first edit makes an ordinary acquire request (not `renewOnly`). A
successful resource save releases that client's exclusive document lease within
the save transaction; a failed transaction retains it. Canvas leases retain their
existing lifecycle. Status responses distinguish `available`, `acquired`, and
`shared`, allowing idle editors to become writable on the next 3-second poll.
A forced unlock retains the established shared-editing policy: all clients may
continue editing and saving, and the last save wins. It does not revoke another
client's draft or switch the document back to exclusive editing.
