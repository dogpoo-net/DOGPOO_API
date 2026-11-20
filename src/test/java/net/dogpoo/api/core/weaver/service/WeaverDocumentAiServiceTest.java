package net.dogpoo.api.core.weaver.service;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class WeaverDocumentAiServiceTest {
    @Test
    void sendsOnlyRequestedSourcesAndBoundsCompletion() {
        final var payload = WeaverDocumentAiService.payload(
            List.of("첫 생각", "두 번째 생각"), "사용자 자료", "공유용", "short");
        assertEquals("openai/gpt-oss-20b", payload.get("model"));
        assertEquals(512, payload.get("max_completion_tokens"));
        final var messages = (List<?>) payload.get("messages");
        assertEquals(5, messages.size());
        assertTrue(messages.get(1).toString().contains("공유용"));
        assertTrue(messages.get(4).toString().contains("사용자 자료"));
    }

    @Test
    void rejectsEmptyOversizedAndUnknownLengthInputs() {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
            () -> WeaverDocumentAiService.payload(List.of(" "), "", "", "medium")).getStatusCode());
        assertThrows(ResponseStatusException.class,
            () -> WeaverDocumentAiService.payload(List.of("가".repeat(20001)), "", "", "long"));
        assertThrows(ResponseStatusException.class,
            () -> WeaverDocumentAiService.payload(List.of("내용"), "", "", "unlimited"));
    }
}
