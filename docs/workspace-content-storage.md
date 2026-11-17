# Workspace item content storage

## Scope

Migration 023 separates content by domain while preserving the existing client protocol,
workspace/item identifiers, revisions, author metadata, document merge and history behavior.
This is a bounded vertical/domain split, not a claim that all JSON has been normalized.

| Table | Content |
| --- | --- |
| WEAVER_WORKSPACE_ITEM | Sync index: workspace, hashed key, kind, original id, revision, tombstone, UTF-8 byte count |
| WEAVER_MEMO_CONTENT | Active and trash memo records |
| WEAVER_DOCUMENT_CONTENT | Document records (`list:notes`) |
| WEAVER_CANVAS_LAYOUT | Individual canvas layout values |
| WEAVER_WORKSPACE_STRUCTURE | Boxes, folder records and folder relationships |
| WEAVER_WORKSPACE_SETTING | Colors, scalar values, other/forward-compatible settings |

Each content table has a `(workspace_id,item_key)` primary key and cascading foreign key
to the sync index. Workspace/account deletion consequently deletes content as before.
Metadata, comments, files, membership and document history remain in their dedicated tables.
The legacy `payload` column remains NULL only to support explicit rollback; the new API never
reads it. Contents retain their existing JSON representation to avoid loss of client fields.
Further field normalization should follow concrete query requirements.

## Read/write behavior

- The workspace transaction and row lock cover index + typed content updates atomically.
- Fixed Java routing selects table names; request input cannot become a SQL identifier.
- `changes` starts with the `(user_id,revision)` index and joins content by primary key.
- Single-item reads retain the existing key; no client migration/re-download is required.
- Quotas sum `payload_bytes` plus media/history, rather than reading every content JSON.
- Delete removes the content row but keeps the sync tombstone. Restoring a memo writes its
  content again through the same API. Trash content stays until actual deletion.
- Unknown item kinds retain compatibility through the settings table.
- Sync tombstones must not be purged without a separate cursor-expiry/full-resync protocol.
  This migration intentionally does not introduce unsafe garbage collection.

## Deployment and recovery

1. Build/test the API and stage it with both migration scripts and `tool/weaver-db-local.sh`.
2. Stop `weaver-api.service` to prevent old writers during migration.
3. Execute `023-weaver-item-content-split.sql` through the existing server-local DB helper.
   DDL is idempotent. Backfill, binary-content comparison and clearing legacy payloads are
   one transaction. On mismatch the procedure rolls back and raises an error.
4. Install the new jar and start the service. Inspect DB integrity summaries, not payloads.
5. If application deployment fails, stop the service, run
   `023-weaver-item-content-rollback.sql`, restore the prior jar, then start it.
   Do not run the old jar against empty legacy payloads.
6. To retry after rollback, stop writers and rerun the forward migration. It overwrites
   child content from current legacy payloads and removes content for deleted items.

DDL survives a failed migration, but is additive; the original payloads remain intact.
No table drops or permanent backup of user data are needed for this migration.
Physical disk-space reclamation is not guaranteed by NULLing the old column; an offline
InnoDB table rebuild would be a separate maintenance decision if measurements justify it.

## Reproducible integration validation

Use a disposable MariaDB 10.11 container named `weaver-content-split-test`, localhost port
33317, database `weaver_split`, root password `local-split-test` (test-only). With Java 25
and Maven on PATH run `bash tool/test-content-split.sh`. It seeds synthetic UTF-8 data,
runs migration twice, verifies real MyBatis routing/read/write/delete/restore/rollback,
checks byte quotas and FK cascading, then rolls back the schema content and repeats.
Never point this fixture at the production database. Remove the container afterward.
