package sh.miles.powerai.data.objs;

import java.time.Instant;
import java.util.UUID;

public record ScriptStoreEntry(String name, UUID owner, String script, Instant publishedAt) {
}
