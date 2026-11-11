# Additional identity providers

The Flutter login page currently marks Apple, Kakao and Naver as unavailable.
They do not issue app sessions until provider registration and server-side token
exchange/validation have been implemented. Never embed provider secrets in Flutter.

| Provider | Required registration and settings |
| --- | --- |
| Apple | Apple Developer membership; Sign in with Apple-enabled App ID; web Services ID; registered domains/HTTPS return URL; Team ID, Key ID and private .p8 key for server-generated client secrets. Native iOS also requires bundle ID and entitlement. Support Apple's private relay email and key rotation. |
| Kakao | Kakao Developers app; Kakao Login enabled; platform/domain and redirect URI registration; REST API key and configured client secret; consent scopes for profile/email. Email availability must be checked and cannot be assumed. Production/business eligibility depends on requested permissions. |
| Naver | Naver Developers application for Naver Login; web/native service settings and callback URL; Client ID and Client Secret; requested profile scopes and production review requirements. |

The backend must perform state/nonce checks as applicable, authorization-code
exchange, token/signature/issuer/audience validation and link accounts by stable
provider subject. Do not silently link a social account to an email account merely
because their email strings match. Domain/redirect settings must match the final
API domain. Provider secrets belong only in server environment configuration.
