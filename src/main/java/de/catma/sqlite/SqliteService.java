package de.catma.sqlite;

import de.catma.properties.CATMAPropertyKey;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SqliteService {
    private final Logger logger = Logger.getLogger(SqliteService.class.getName());

    private final String sqliteDbBasePath = CATMAPropertyKey.SqliteDbBasePath.getValue();

    public static class SqliteModel {
        public static class Notice {
            public Integer id;
            public String message;
            public Instant dateCreated;
            public Instant dateStart;
            public Instant dateEnd;
            public Boolean isIssue;

            public Notice(Integer id, String message, Instant dateCreated, Instant dateStart, Instant dateEnd, Boolean isIssue) {
                this.id = id;
                this.message = message;
                this.dateCreated = dateCreated;
                this.dateStart = dateStart;
                this.dateEnd = dateEnd;
                this.isIssue = isIssue;
            }
        }
    }

    private Connection getConnection() throws IOException, SQLException {
        Path sqliteDbFilePath = Paths.get(sqliteDbBasePath, "catma.db");
        return DriverManager.getConnection("jdbc:sqlite:" + sqliteDbFilePath.toAbsolutePath());
    }

    public SqliteService() throws IllegalStateException {
        logger.log(Level.INFO, "SQLite initializing");

        try {
            Class.forName("org.sqlite.JDBC");

            File dbPath = new File(sqliteDbBasePath);
            if (!dbPath.exists()) {
                dbPath.mkdir();
            }
        }
        catch (ClassNotFoundException e) {
            throw new IllegalStateException("Failed to initialise SqliteService", e);
        }

        try (
                Connection connection = getConnection();
        ) {
            Statement statement = connection.createStatement();
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS notice (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "message TEXT NOT NULL," +
                    "date_created TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))," +
                    "date_start TEXT," +
                    "date_end TEXT," +
                    "is_issue INTEGER NOT NULL DEFAULT 0" +
                    ")"
            );
        }
        catch (IOException | SQLException e) {
            throw new IllegalStateException("Failed to initialise Database", e);
        }
    }

    public ArrayList<SqliteModel.Notice> getNotices() {
        ArrayList<SqliteModel.Notice> notices = new ArrayList<>();

        try (
                Connection connection = getConnection();
        ) {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("SELECT * FROM notice WHERE (" +
                    "(date_start IS NULL OR date_start <= strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))" +
                    "AND (date_end IS NULL OR date_end > strftime('%Y-%m-%dT%H:%M:%fZ', 'now')))" +
                    "ORDER BY date_start DESC"
            );

            while (rs.next()) {
                String dateStart = rs.getString("date_start");
                String dateEnd = rs.getString("date_end");

                notices.add(new SqliteModel.Notice(
                        rs.getInt("id"),
                        rs.getString("message"),
                        Instant.parse(rs.getString("date_created")),
                        dateStart != null ? Instant.parse(dateStart) : null,
                        dateEnd != null ? Instant.parse(dateEnd) : null,
                        rs.getBoolean("is_issue")
                ));
            }

            return notices;
        }
        catch (IOException | SQLException e) {
            logger.log(Level.SEVERE, "Couldn't fetch notices: " + e.getMessage());
            return notices;
        }
    }
}
