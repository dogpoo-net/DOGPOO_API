# Account notifications

Apply `docs/sql/028-weaver-account-notifications.sql` before deploying this API.
`WEAVER_ACCOUNT_NOTIFICATION` stores account-scoped inbox rows and read timestamps.

- Pending invitations are materialized on inbox/count requests, including existing valid invitations. No email or token is returned by this API. Accepting still uses the authenticated invite acceptance endpoint.
- New comments notify the workspace owner and current members, excluding the author. The original comment request ID deduplicates retries. Existing comments are not backfilled.
- Subscription changes and cancel/resume actions notify the subscription account. Expiration is materialized when the inbox is queried and deduplicated by the subscription end timestamp; no sleep or additional scheduler is used.
- Membership, workspace archival, memo deletion, invitation expiry/revocation and account ownership restrict visibility. Account/workspace deletion cascades inbox records. Notifications contain generic summaries rather than comment content.
- `/notifications`: POST authenticated session endpoint under `/workspace`; accepts `before` (exclusive ID cursor) or `countOnly`. Page size 50.
- `/notifications/read`: POST `{id}` or `{id, all:true}`. Always constrained to authenticated account; read-all stops at the latest row the user has seen, leaving newer arrivals unread.
- Client refreshes unread counts on mount/resume and once per minute while active; opening the modal fetches the latest page. Read status persists across devices. Background polling pauses when the app is inactive.

Tests: service ownership/cursor checks and frontend badge/read/navigation tests. Schema SQL verified on DOGPOO before API deployment. Notifications do not replace email delivery or initiate push notifications to OS devices.
