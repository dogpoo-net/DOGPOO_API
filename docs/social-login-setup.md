# Social login setup

Kakao, Naver, GitHub and GitLab are implemented using the flows documented in
[session-policy.md](session-policy.md). Apple is hidden. Client secrets are server
configuration only. Never copy them into Flutter defines, source or handoff docs.

| Provider | Console registration |
| --- | --- |
| Kakao | App ID, native and JavaScript public keys; production web domain; Android package `net.dogpoo.weaver` + actual debug/release signing key hashes; iOS bundle `net.dogpoo.weaver` and `kakao<native-key>` URL scheme. Nickname/email consent required; photo optional. |
| Naver | Client ID/Secret; required name/email; callback `https://api.dogpoo.net/dogpoo/weaver/api/auth/naver/callback`. |
| GitHub OAuth App | Client ID/Secret; callback `https://api.dogpoo.net/dogpoo/weaver/api/auth/github/callback`; identity scopes only. |
| GitLab confidential OAuth application | Application ID/Secret; callback `https://api.dogpoo.net/dogpoo/weaver/api/auth/gitlab/callback`; openid/profile/email. Current instance: gitlab.com. |

Kakao uses the SDK's native/JavaScript keys, not the REST API client secret.
Kakao is not supported by its Flutter SDK on Windows. Other new providers use the
system browser on Windows/mobile and the same server callback, so no client secret
or custom desktop URL protocol is needed. Rebuild installed apps to ship the new
plugins and platform callback configuration. Console registration/review and actual
user consent must be checked with each provider before general availability.
