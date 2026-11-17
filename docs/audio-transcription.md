# Audio transcription adapter

Authenticated POST /weaver/api/auth/session/{web|native}/workspace/audio/transcribe
uses JSON {"workspaceId":"current-space","data":"base64 audio bytes"}.
Web uses the existing session cookie and approved Origin; native uses its bearer session.
Workspace membership/edit permission is checked before processing.

Response: text, provider, model, durationSeconds, transcribedSeconds, truncated.
Free: recording at most 60 seconds, only first 10 seconds transcribed.
Paid workspace: at most 3600 seconds, full transcription. Input limit 32MiB; compressed
provider upload limit 24MiB. Duration is inspected server-side, not trusted from the client.

Backoffice offers Groq whisper-large-v3-turbo / whisper-large-v3 and OpenAI
 gpt-4o-mini-transcribe. Admin configuration is stored in WEAVER_AUDIO_SETTING and audited.
Default disabled. Configure GROQ_API_KEY or OPENAI_API_KEY in the server environment.
Install ffmpeg and ffprobe, or set WEAVER_FFMPEG and WEAVER_FFPROBE executable paths.
No secret is returned to the browser or included in the workbook. Activation requires both
provider credentials and runtime tools. The current test server has neither runtime tool on PATH.

Conversion runs in isolated temporary directories, restricts protocols/formats, has timeouts,
and removes temporary input/output. Concurrency is bounded to two conversions and requests are
rate-limited per account. Provider destinations/models are allowlisted. This is the backend API
and administrator selector; recording UI does not yet automatically call transcription.

Tests use synthetic audio and a mocked provider HTTP response. No billable live provider test
has been performed. For Cloudflare Workers, ffmpeg subprocess execution needs replacement with
a supported media preprocessing service/client encoding before this adapter can be migrated.
