package sh.miles.powerai.data;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import sh.miles.powerai.data.objs.ScriptStoreEntry;
import sh.miles.powerai.util.PlayerStorage;
import sh.miles.powerai.util.SQLUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Every published script, readable newest-first a page at a time or per player. All methods run on the database
 * thread and return futures; they complete off the main thread, so hop back with the scheduler before touching Bukkit.
 * Reads are cached and every write invalidates what it could have changed.
 */
public final class MarketplaceStore {

    /**
     * One page of the marketplace. {@code number} is 0-based
     */
    public record Page(int number, List<ScriptStoreEntry> entries, boolean hasNext) {
        public boolean hasPrevious() {
            return number > 0;
        }
    }

    private final Logger logger;
    private final Supplier<Connection> connection;
    private final Executor executor;
    private final int pageSize;

    private final AsyncLoadingCache<UUID, List<ScriptStoreEntry>> byOwner;
    private final AsyncLoadingCache<Integer, Page> pages;

    MarketplaceStore(Logger logger, Supplier<Connection> connection, Executor executor, int pageSize) {
        if (pageSize < 1) throw new IllegalArgumentException("pageSize must be at least 1");
        this.logger = logger;
        this.connection = connection;
        this.executor = executor;
        this.pageSize = pageSize;

        this.byOwner = Caffeine.newBuilder()
                .executor(executor)
                .maximumSize(500)
                .expireAfterWrite(Duration.ofMinutes(5))
                .buildAsync(this::queryByOwner);

        this.pages = Caffeine.newBuilder()
                .executor(executor)
                .maximumSize(100)
                .expireAfterWrite(Duration.ofSeconds(30))
                .buildAsync(this::queryPage);
    }


    public CompletableFuture<Page> page(int number) {
        return pages.get(Math.max(0, number));
    }


    public CompletableFuture<List<ScriptStoreEntry>> byOwner(UUID owner) {
        return byOwner.get(owner);
    }

    public CompletableFuture<ScriptStoreEntry> publish(UUID owner, String name, String script) {
        // names end up in store UI text, so only ever store names a player could have saved
        if (!PlayerStorage.isValidScriptName(name)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("invalid script name '" + name + "'"));
        }

        @Language("sqlite") final String upsert = """
                INSERT INTO script_store (name, owner, script, published_at)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (name, owner) DO UPDATE SET script       = excluded.script,
                                                        published_at = excluded.published_at
                """;

        final ScriptStoreEntry entry = new ScriptStoreEntry(name, owner, script, Instant.now());
        return run(() -> {
            try (PreparedStatement statement = connection.get().prepareStatement(upsert)) {
                statement.setString(1, entry.name());
                statement.setBytes(2, SQLUtils.toBytes(entry.owner()));
                statement.setString(3, entry.script());
                statement.setLong(4, entry.publishedAt().toEpochMilli());
                statement.executeUpdate();
            }
            return entry;
        }).whenComplete((ignored, error) -> invalidate(owner));
    }

    public CompletableFuture<Boolean> unpublish(UUID owner, String name) {
        @Language("sqlite") final String delete = """
                DELETE FROM script_store WHERE owner = ? AND name = ?
                """;

        return run(() -> {
            try (PreparedStatement statement = connection.get().prepareStatement(delete)) {
                statement.setBytes(1, SQLUtils.toBytes(owner));
                statement.setString(2, name);
                return statement.executeUpdate() > 0;
            }
        }).whenComplete((ignored, error) -> invalidate(owner));
    }

    private void invalidate(UUID owner) {
        byOwner.synchronous().invalidate(owner);
        pages.synchronous().invalidateAll();
    }

    private List<ScriptStoreEntry> queryByOwner(UUID owner) throws SQLException {
        @Language("sqlite") final String query = """
                SELECT name, owner, script, published_at
                FROM script_store
                WHERE owner = ?
                ORDER BY name
                """;

        try (PreparedStatement statement = connection.get().prepareStatement(query)) {
            statement.setBytes(1, SQLUtils.toBytes(owner));
            return readEntries(statement);
        }
    }

    private Page queryPage(int number) throws SQLException {
        @Language("sqlite") final String query = """
                SELECT name, owner, script, published_at
                FROM script_store
                ORDER BY published_at DESC, owner, name
                LIMIT ? OFFSET ?
                """;

        try (PreparedStatement statement = connection.get().prepareStatement(query)) {
            statement.setInt(1, pageSize + 1);
            statement.setLong(2, (long) number * pageSize);
            List<ScriptStoreEntry> entries = readEntries(statement);

            boolean hasNext = entries.size() > pageSize;
            if (hasNext) entries = entries.subList(0, pageSize);
            return new Page(number, List.copyOf(entries), hasNext);
        }
    }

    private static List<ScriptStoreEntry> readEntries(PreparedStatement statement) throws SQLException {
        final List<ScriptStoreEntry> entries = new ArrayList<>();
        try (ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                entries.add(new ScriptStoreEntry(
                        result.getString("name"),
                        SQLUtils.fromBytes(result.getBytes("owner")),
                        result.getString("script"),
                        Instant.ofEpochMilli(result.getLong("published_at"))
                ));
            }
        }
        return entries;
    }

    @FunctionalInterface
    private interface SqlTask<T> {
        T run() throws SQLException;
    }

    private <T> CompletableFuture<T> run(SqlTask<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.run();
            } catch (SQLException e) {
                logger.error("marketplace write failed", e);
                throw new CompletionException(e);
            }
        }, executor);
    }
}
