package net.dogpoo.api;
import org.junit.jupiter.api.Test;import net.dogpoo.api.core.weaver.service.WeaverHistoryService;import static org.junit.jupiter.api.Assertions.*;
class WeaverHistoryTest {
 @Test void koreanAndEmojiAreCountedAsCodePoints(){var diff=WeaverHistoryService.difference("첫 줄\n😀가나다","첫 줄\n😃가나!");assertEquals(2,diff.added());assertEquals(2,diff.removed());assertTrue(diff.chunks().stream().anyMatch(c->c.get("operation").equals("insert")&&c.get("afterLine").equals(2)));}
}
