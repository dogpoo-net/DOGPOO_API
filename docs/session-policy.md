# Weaver server sessions and social login

Apply SQL migrations `001` through `006` in `docs/sql` before deploying the API.
They add authentication tables and a nullable display name for existing accounts;
no workspace data or existing account IDs are replaced. Schema initialization
remains disabled. New email signup requires `name`, `email`, `password` and
`passwordConfirmation`; names are trimmed and limited to 80 Unicode code points.

## Session policy

| Client | Credential | Expiration / storage |
| --- | --- | --- |
| Windows / Android / iOS | Opaque Access Token | 30 minutes; client memory only |
| Windows / Android / iOS | Rotating Refresh Token | 90-day idle timeout; absolute 365 days from login |
| Web | `__Host-weaver-<tab>` cookie + tab proof | HttpOnly, Secure, SameSite=Strict, Path=/; no Max-Age/Expires |

Native refresh credentials survive app termination, reboot and 30 days without
use, provided the same OS user/device storage remains available and the session
has not been revoked. Windows uses Flutter Secure Storage's Windows Credential
Manager-backed encryption; Android uses Keystore-wrapped AES-GCM storage; iOS uses
Keychain `first_unlock_this_device` with iCloud synchronization disabled. Android
secure preference files are excluded from backup and device transfer.

A successful refresh or authenticated session lookup advances idle expiry but
never the absolute deadline. The client refreshes after 25 minutes and on resume.
Network errors preserve stored refresh credentials; confirmed 401 clears them.
Server logout must succeed before the client reports success. Offline logout is
not represented as successful server revocation. Account-wide logout deletes all
server device sessions and their access tokens, including legacy server sessions.

Refresh tokens are random opaque secrets; only hashes are stored in the DB. A
rotation request has a random idempotency ID stored alongside the old token in
secure storage before transmission. The same old token/request pair can recover
its exact replacement after a lost response or app shutdown. The replacement is
HMAC-derived using a domain-separated server key (`jwt.secret`); no plaintext
refresh token is retained server-side. Reuse of the immediately previous token
with a different request ID revokes the device family. Keep the rotation key
stable across rolling deployments. Older tokens fail authentication.

Each web tab stores only a random tab ID and proof in sessionStorage. The secret
cookie is unreadable to JavaScript. Refresh restores the tab using cookie + proof;
a normal new tab after closing the old one has no proof and cannot restore it.
Browser session/tab restoration and duplicated tabs can restore/copy sessionStorage;
web platforms cannot guarantee logout on every possible close/restore operation.
No unload logout request is used because reload also unloads the page. Closing a
tab does not guarantee immediate server deletion; unreachable web records expire
after 24 hours of inactivity (absolute 365-day cap). Explicit logout revokes them
immediately. An hourly job removes expired auth rows; expiry is checked on every
request independently of physical cleanup.

Cookie routes require the exact `WEAVER_WEB_ORIGIN` (default
`https://weaver.dogpoo.net`) and X-Weaver-Tab/X-Weaver-Proof headers. Cookies are
host-only on the API host. The frontend/API must be HTTPS and same-site for the
Strict cookie policy. Cloudflare preview / github.io / localhost origins do not
inherit production cookie authorization. Existing CORS configuration must also
allow the selected origin. Rate limits are process-local; configure a trusted
shared edge limiter before scaling instances.

## Providers and account linking

Kakao uses the official Flutter SDK on web/Android/iOS. Its transient provider
tokens remain in memory only and are exchanged for Weaver sessions. The server
checks Kakao app ID, token expiry, subject identity and verified email. The SDK
has no Windows implementation; that button is disabled there.

Naver, GitHub and GitLab use server-side authorization-code exchange. Native apps
open the system browser; web opens a popup. A 10-minute DB flow binds the provider,
random state, random handoff ID and client proof. Only the original client can
finish, and completion is single-use. Callback responses never contain tokens;
provider tokens are not persisted. GitHub scopes are `read:user user:email`;
GitLab scopes are `openid profile email`. No repository access is requested.

Provider subject IDs, not email strings, identify linked accounts. Matching emails
never auto-link. Log in using the existing method and explicitly select the
provider's account-link action in settings. Linking rechecks the current server
session and rejects a provider already bound to another account. New social
accounts receive the provider name and a verified email; their password is a
random unknowable hash, not a provider token or a usable email-login password.
There is no password-set/reset UI in this change.

Required server settings (never put secrets in Flutter):

* KAKAO_LOGIN_APP_ID
* NAVER_CLIENT_ID, NAVER_CLIENT_SECRET
* WEAVER_GITHUB_CLIENT_ID, WEAVER_GITHUB_CLIENT_SECRET
* WEAVER_GITLAB_CLIENT_ID, WEAVER_GITLAB_CLIENT_SECRET
* Optional WEAVER_GITLAB_BASE_URL defaults to https://gitlab.com.

Callbacks are `https://api.dogpoo.net/dogpoo/weaver/api/auth/{provider}/callback`,
where provider is naver, github or gitlab. Optional NAVER_CALLBACK_URL,
WEAVER_GITHUB_CALLBACK_URL and WEAVER_GITLAB_CALLBACK_URL override those defaults.
Kakao SDK account popup uses its JS-SDK redirect flow; the REST API client secret
is not used by that SDK exchange. Register the production web domain, Android
package and signing key hashes, and iOS bundle/scheme with Kakao.

## Compatibility and validation boundary

The pre-existing Google client-only remembered-login marker remains a test
implementation; it does not become a server refresh session in this change.
Legacy email-address-only test backup endpoints also remain legacy APIs. These
changes do not claim server authorization for those endpoints. New session
revocation is enforced by the new authenticated session routes.

`mvn verify` covers ordinary tests without a database. With explicitly configured
DB credentials and the migrations, run:

```
WEAVER_AUTH_DB_TEST=true mvn -Dtest=WeaverAuthDatabaseTest,WeaverSessionDatabaseTest,WeaverSocialDatabaseTest test
```

These tests create unique example.invalid accounts, mock mail/provider gateways,
and delete their own rows. They cover real SQL, clock-driven expiration, rotation
retry/replay, device/global revocation, tab proof, OAuth state/provider binding,
one-use completion and explicit account linking. They do not exercise a real
person's provider consent screens. Native OS storage/signing requires testing on
the actual Windows/Android/iOS device; Linux unit tests use an injected store.
