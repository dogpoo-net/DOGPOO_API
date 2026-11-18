-- Apply after GROQ_API_KEY and WEAVER_FFMPEG/WEAVER_FFPROBE are configured.
-- AI entitlement is checked against the requesting account's active subscription.
UPDATE WEAVER_AUDIO_SETTING
SET provider='groq', model='whisper-large-v3-turbo', enabled=1
WHERE id=1;
