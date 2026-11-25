package sh.miles.powerai.data;

import com.google.common.base.Preconditions;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class DataStore {
    private static final List<String> MIGRATIONS = List.of(
            "db/schema.sql"
    );

    private final Plugin plugin;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(Thread.ofPlatform().name("powerai-db").factory());

    private Connection connection = null;
    private MarketplaceStore marketplace;

    public DataStore(Plugin plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        Path path = plugin.getDataPath().resolve("data.db");
        try {
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath());
            migrate();

            this.marketplace = new MarketplaceStore(this.plugin.getSLF4JLogger(), () -> this.connection, this.executor, 15);
        } catch (SQLException | IOException e) {
            plugin.getSLF4JLogger().error("failed to set up the database", e);
            plugin.getServer().shutdown();
        }
    }

    public MarketplaceStore getMarketplace() {
        Preconditions.checkArgument(this.connection != null, "the database has not been connected too");
        return marketplace;
    }

    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                plugin.getSLF4JLogger().warn("database queries didn't finish within 5s, closing anyway");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (this.connection == null) return;
        try {
            this.connection.close();
        } catch (SQLException e) {
            plugin.getSLF4JLogger().error("failed to close db connection", e);
        }
        this.connection = null;
    }

    private void migrate() throws SQLException, IOException {
        int version = userVersion();
        if (version > MIGRATIONS.size()) {
            throw new IllegalStateException("data.db is at schema version " + version + " but this build only knows up to "
                    + MIGRATIONS.size() + ", was it made by a newer version of the plugin?");
        }

        for (int i = version; i < MIGRATIONS.size(); i++) {
            String file = MIGRATIONS.get(i);
            int target = i + 1;
            plugin.getSLF4JLogger().info("migrating data.db to schema version {} ({})", target, file);

            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                for (String sql : statements(readResource(file))) {
                    statement.execute(sql);
                }
                statement.execute("PRAGMA user_version = " + target);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw new SQLException("migration " + file + " failed", e);
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private int userVersion() throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("PRAGMA user_version")) {
            return result.next() ? result.getInt(1) : 0;
        }
    }

    private String readResource(String file) throws IOException {
        try (InputStream in = plugin.getResource(file)) {
            if (in == null) throw new IOException("missing resource " + file);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static List<String> statements(String sql) {
        StringBuilder stripped = new StringBuilder();
        for (String line : sql.split("\n")) {
            int comment = line.indexOf("--");
            stripped.append(comment >= 0 ? line.substring(0, comment) : line).append('\n');
        }

        List<String> statements = new ArrayList<>();
        for (String part : stripped.toString().split(";")) {
            if (!part.isBlank()) statements.add(part.strip());
        }
        return statements;
    }
}
