# Weaver email authentication

Email signup requires name, email and matching passwords. Passwords use BCrypt
(10+ characters, maximum 72 UTF-8 bytes). Verification links are 256-bit random
secrets, stored only as SHA-256 hashes, valid for 30 minutes and consumed once.
Resend replaces the link with a 60-second cooldown. SMTP failure rolls back the
signup/token transaction. SMTP uses TLS port 465 and hostname validation.

Configure DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET and
SMTP_HOST/SMTP_FROM/SMTP_USERNAME/SMTP_PASSWORD on the server. WEAVER_PUBLIC_URL
defaults to `https://weaver.dogpoo.net/`. No secrets belong in source or the web build.

Under `/dogpoo/weaver/api/auth`, POST signup/resend/verify retain their contracts
except that signup now requires `name`. The v1 login/me/logout endpoints remain
for compatibility; the current client uses `/session/native/...` or
`/session/web/...`. See [session-policy.md](session-policy.md) for lifetimes,
secure storage, cookie handling, all-device logout and social account linking.
Existing names remain null until supplied; existing account IDs and data remain.
