# Account subscriptions and workspaces

Subscriptions belong to an account. Every owned workspace inherits the owner's effective plan.
Joined spaces consume no ownership slot and use their owner's plan, regardless of participant plan.

| Plan | Active owned spaces | Storage mode |
|---|---:|---|
| Free | 2 | Local offline |
| Standard | 10 | Online |
| Pro | Unlimited | Online |

Free creation is limited to one per rolling 24 hours. Signup's initial creation counts;
deleting a space never resets the timestamp. Existing creation timestamps are not invented
at migration. Restore/transfer enforce ownership quota; creation alone enforces cooldown.
Zero owned spaces is valid and never causes automatic regeneration.

Free owners can read existing server data but cannot write/synchronize/share it. Joined paid
spaces remain online. Offline caches are scoped by account AND workspace, separate from online
caches. Logout preserves those isolated caches. A clean offline copy can be refreshed from the
online cache; dirty copies are preserved. Upgrading prompts before uploading offline changes.
Offline operation after entry uses local storage; account login/workspace management still need
server access. Cached owned offline catalogs allow reopening on catalog network failure.

File export includes saved workspace contents and attachments. Import validates before changes,
asks for destructive confirmation and replaces ONLY the current workspace. Free replaces local
contents. Paid owner import checks the current server revision, covers all existing items and
atomically replaces that workspace. Other spaces remain unchanged. Failed paid imports preserve
existing content; staged uploads can remain for later cleanup. Import does not change global theme.

All workspace deletion archives for 30 days. Used server spaces require owner email verification;
unused spaces do not. Offline-only data must be backed up on the device: server retention does not
create a server copy of local data. Restore requires an active paid owner and available quota.
Expiry purge runs daily at 00:00 Asia/Seoul. Previous deadlines are preserved.

Cancellation preserves paid rights through period_end; requests evaluate expiry using DB UTC.
At expiry owned spaces become Free/offline; data is not erased. Excess spaces are read-only,
with owner-selectable priority. Shared access resumes on renewal. Storage/document/paid-editor
limits remain configurable and unset until decided; operational 64MiB safeguards remain.

Administrators can manage account roles, account subscription/expiry and member roles. Last-admin
removal is forbidden; mutations are audited. Backoffice exit returns to the prior workspace.
Initial administrator: dogpoo.net@gmail.com. No payment checkout, billing or webhook is connected.

Apply migrations 026 and 027 before the matching API. Migration 026 transfers the strongest active
legacy workspace grant per owner once, guarded by WEAVER_POLICY_MIGRATION. Legacy grants remain
for rollback but are no longer entitlement authority. Rollback requires restoring the previous API
and web assets; avoid mutations while resolving a deployment failure.

QA workbook: /home/ocaptain/WEAVER_workspace_policy_tests.xlsx.
Generator: tool/generate_policy_tests.py. Manual/device cases require separate execution.
