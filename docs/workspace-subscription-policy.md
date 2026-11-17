# Workspace, subscription and administrator policy

## Agreed behavior
- A newly registered email/social account receives `기본 워크스페이스` once. Existing users are
  marked as provisioned by migration 025. Catalog/read/last deletion never recreate a space.
- Zero owned spaces is valid. Joined spaces do not consume ownership quota. Zero accessible
  spaces shows creation/restoration/invitations, not a guest workspace or old account data.
- Last-space deletion and transfer are allowed. Creation, restoration and ownership receipt
  check the recipient's configured active owned-space limit under an account row lock.
- Free deletion is irreversible after owner email verification when used; unused spaces can
  be removed directly. Logical DB access ends immediately; physical media deletion uses the
  existing retryable erasure queue. Paid used-space deletion retains the existing 30-day
  deadline. Previously archived spaces keep their deadline, including after downgrade.
- Restore requires an active paid subscription on the archived workspace and passes both
  ownership and content quota checks. Daily expiry purge remains 00:00 Asia/Seoul.
- A workspace subscription applies to all members according to their roles, regardless of
  whether a member owns a paid workspace elsewhere.
- Cancellation sets `cancel_at_end`, preserves the period end and paid rights until that
  instant. Resuming clears the cancellation reservation; it never extends the period.
- Entitlement queries use UTC database time on every request. At expiry the effective plan
  is Free even without a scheduled worker. No data deletion accompanies downgrade.
- No payment provider is connected. There are no automatic renewals, charges or refunds.
  Admin grants/period extensions are server-managed test/operations entitlements only.

## Limits and downgrade
`WEAVER_PLAN_POLICY`: `owned_limit`, `storage_limit` (bytes), `document_limit`, `editor_limit`.
NULL means not configured/no additional plan limit. Only Free editor_limit=2 is seeded.
The preexisting operational 64 MiB workspace safeguard remains; plan limits do not bypass it.
Ownership limit is the largest configured limit among Free and the user's currently active
owned-space plans. If any applicable limit is NULL no ownership restriction is added.
Archived and joined spaces do not count. No arbitrary Standard/Pro limits were invented.

When configured limits are exceeded:
- Existing content remains readable. Existing docs may be edited without increasing doc count.
- Current content bytes may decrease; plan-level storage growth is rejected transactionally.
  Plan bytes measure current payload+media; existing history has the separate operational cap.
- Excess owned spaces are read-only except deletion cleanup. Owner can select priority spaces;
  deterministic priority/order provides a safe fallback when none is chosen after expiration.
- Free keeps owner plus one effective editor. Other configured editors are effectively viewers;
  persisted member roles are not silently destroyed. Stable joined order determines fallback.
  Owner can explicitly select retained editors. Upgrading restores configured role eligibility.
- Pending editor invitations still reserve seats; accepting/promoting cannot exceed the limit.
- Client state may need reopening a workspace after role/plan changes. Server checks are always
  authoritative and cannot be bypassed by stale tabs or crafted requests.

## Backoffice
Only `platform_role=admin` can call any `admin/*` API. Server sessions/Origin protections apply.
The settings link is shown for admin accounts; hiding it is not the authorization mechanism.
`나가기` pops the backoffice route to the workspace the user was viewing; no logout or switch.
Account detail controls general/admin role and each owned workspace's subscription separately.
Workspace member roles editor/viewer can be adjusted, but not the owner through that endpoint.
The last administrator cannot be demoted. All mutations are transactional and audited in
`WEAVER_POLICY_AUDIT` (actor, target, action, prior/new state, time).
Account deletion nulls audit actor references; audit records contain IDs/role/plan changes,
not passwords, tokens or message contents. Audit retention must be finalized before production.

Initial administrator: the existing verified `dogpoo.net@gmail.com` account, installed by a
one-time server-side SQL step, never by an unauthenticated bootstrap HTTP route.

## Deployment
1. Stop API writers, apply additive migration 025, bootstrap the approved administrator,
   install the built API jar, start it, then deploy the matching Flutter web assets.
2. Do not delete existing archived spaces or change their deadlines during migration.
3. For rollback, restore old API/web builds; additive tables/columns may remain. Rollback also
   restores the old lifecycle semantics, so avoid performing workspace mutations until fixed.
4. Set numeric limits deliberately in WEAVER_PLAN_POLICY after product decisions, then test
   downgrade cases on synthetic test accounts before enabling in production.
5. Workbook: `/home/ocaptain/WEAVER_workspace_policy_tests.xlsx`; generation script in
   `tool/generate_policy_tests.py`. Manual/browser cases are explicitly not marked as passed.

## Follow-up scope
Payment checkout/webhooks/signature validation/idempotency/reconciliation, refunds,
production plan numbers/pricing, billing/legal retention and administrator audit retention
are not claimed as completed. Future payment events should update subscription state through
an authenticated provider adapter and must not accept plan grants from ordinary users.
