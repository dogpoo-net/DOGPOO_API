package net.dogpoo.api.core.weaver.model;

import java.time.LocalDateTime;

public record WeaverAccount(String id, String email, String passwordHash, LocalDateTime verifiedAt) {}
