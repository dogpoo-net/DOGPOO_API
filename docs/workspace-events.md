# Workspace change notifications

The current DOGPOO Spring Boot deployment is a test environment. Production is
planned for Cloudflare Workers. Keep this wire contract independent of Spring:

- `POST /dogpoo/weaver/api/auth/session/{web|native}/workspace/events`
- JSON body: `{"workspaceId":"..."}`. Same cookie + proof headers / native bearer
  authentication and membership checks as workspace read. No secrets in URLs.
- Response: `text/event-stream`, `Cache-Control: no-cache, no-store, no-transform`,
  `X-Accel-Buffering: no`.
- Events: `ready`, `change`, `ping`, each with `data: {}`. No document data or
  credentials in notifications. `ready` and `change` trigger revision reconciliation.
- Register notification only after the workspace/comment transaction commits.
- Batch bursts within 250ms; heartbeat every 20s. Revalidate session and membership
  before sending. Close after 5 minutes, on authorization failure, or on shutdown.
- No durable event replay is needed: reconnect always reads authoritative DB
  revisions. Never use delivery of a notification as proof of successful save.

Current API traffic goes through HAProxy directly to Spring Boot, with sufficient
idle timeouts; nginx serves the Flutter build. No proxy change was necessary.
For another proxy, disable SSE buffering/caching and allow the heartbeat interval.

The in-memory bus supports this single API instance only. Before production
multi-instance deployment, replace it with workspace-scoped Durable Objects (or
another shared fan-out layer). Preserve the protocol, after-commit notification,
reconnection reconciliation, and access checks. Workers Queues can handle durable
background work separately; they are not a broadcast replacement. No Redis or
Kafka is required for this test deployment and no DB migration is required.

Validation: unit tests cover after-commit delivery, workspace isolation and revoked
access. A local real HTTP test verifies authentication and receiving SSE while the
response remains open. Production HTTP probes are intentionally not performed.
