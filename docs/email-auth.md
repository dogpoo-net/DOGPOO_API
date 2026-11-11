# Weaver email authentication

Implemented in `core.weaver.controller/service/mapper/model`. Existing Google login
and the legacy test-backup endpoints retain their previous behavior. This does not
make the legacy email-address-only backup API an authenticated storage API.

Apply `docs/sql/001-weaver-email-auth.sql` explicitly to the configured database
before deploying. All three tables use InnoDB and the WEAVER_ prefix; no existing
rows or tables are modified. SQL initialization remains disabled.

Routes under `/dogpoo/weaver/api/auth`:

| Method | Route | Input |
| --- | --- | --- |
| POST | signup | email, password, passwordConfirmation |
| POST | resend | email |
| POST | verify | token |
| POST | login | email, password |
| GET | me | Authorization: Bearer sessionToken |
| POST | logout | Authorization: Bearer sessionToken |

Signup only requires email and matching passwords (10+ characters, maximum 72
UTF-8 bytes). Passwords use BCrypt; the confirmation is never stored. Email
addresses are normalized to lowercase. No provider accounts are automatically
merged based on email alone. Existing accounts receive the same signup response.
Verification tokens and sessions contain 256 random bits and only SHA-256 hashes
are stored. Verification expires after 30 minutes and is consumed transactionally;
resend replaces the old link, with a 60-second cooldown. Sessions expire in 12
hours and logout revokes them. The Flutter client keeps sessions in memory only;
page reload requires sign-in. Auth responses use no-store. SMTP failure rolls back
signup/token changes. SMTP uses TLS on port 465 with certificate hostname checks.

Required runtime environment: existing DB_URL/DB_USERNAME/DB_PASSWORD/JWT_SECRET,
SMTP_HOST/SMTP_FROM/SMTP_USERNAME/SMTP_PASSWORD. Optional WEAVER_PUBLIC_URL defaults
to `https://weaver.dogpoo.net/` and must be the trusted HTTPS frontend root.
Verification tokens are carried in a URL fragment, not a request query string;
opening the email does not consume the token until the user presses the button.
Do not put secrets in build arguments, frontend bundles, source or deployment logs.

Per-process rate limits: 100 requests/minute per peer IP and 5/minute per email;
unknown accounts still incur BCrypt comparison work. A shared edge limiter and
trusted proxy configuration are needed before scaling across instances. Pending
signup abuse and password recovery are separate future work; password reset is
not part of this implementation. Configure mail delivery reputation/SPF/DKIM/DMARC
with the actual mail provider for production volume.

Tests: normal `mvn verify` covers validation, hashing, throttling and service
behavior. Opt-in `WEAVER_AUTH_DB_TEST=true mvn -Dtest=WeaverAuthDatabaseTest test`
requires the migration and explicitly configured DB credentials; it creates only
unique `auth-test-...@example.invalid` accounts and removes its own rows afterward.
Real email is mocked in this test. It verifies real SQL, expiry, one-use links,
resend, session revocation and transaction rollback after mail failure.
