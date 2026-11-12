# Weaver account profile API

Apply `docs/sql/010-weaver-account-profile.sql` before deploying. Migration is
additive and repeatable; names remain in WEAVER_USER_ACCOUNT. Profile metadata
is in WEAVER_ACCOUNT_PROFILE and hashed email codes in WEAVER_PASSWORD_CHALLENGE.
For legacy accounts with social links, a successful password login establishes
password capability because older schemas do not record the original signup type.

POST `/weaver/api/auth/session/{web|native}/profile/` actions:
- `read`: name, email, linked providers, emailEnabled, defaultFace, optional avatarUrl/image.
- `name`: `{name}`.
- `photo`: `{image}` base64 PNG/JPEG, max 2 MiB, 4096×4096, normalized to 512×512 PNG.
- `password/request`: sends a code to the current account's verified email.
- `password/change`: `{code,password,passwordConfirmation}`, consumes the code and revokes all sessions.

All routes require existing web origin/tab/proof/cookie checks or native bearer
authentication. The server chooses the account, never a client-supplied account ID.
No profile data or credentials may be cached publicly. Codes are hashed with the
account ID, expire after ten minutes, allow five attempts, and are single use.
Failed attempts commit. Resends are bounded, and failed email sends roll back.

Photos are in `${WEAVER_RESOURCE_DIR}/profile/images/<account UUID>/`. Replaced
files are deleted after commit; a rollback removes the staged new file. Deploy
six default face PNGs from WEAVER_APP_FLUTTER/assets/branding/profile to
`${WEAVER_RESOURCE_DIR}/profile/images/default/`. Clients also bundle those files
for offline fallback. Social avatar URLs are limited to provider CDNs; this server
does not fetch arbitrary remote image URLs. Existing account photos are captured
on their next successful provider login/link, while uploaded photos are preserved.

Google linking is POST `/auth/session/{web|native}/google/link` with `idToken`.
It uses the same verified-provider identity resolution and explicit linking policy
as the other providers, without replacing the current authenticated account.
