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
            public String message;
            public Instant dateCreated;

            public Notice(String message, Instant dateCreated) {
                this.message = message;
                this.dateCreated = dateCreated;
            }
        }
    }

    private Connection getConnection() throws IOException, SQLException {
        Path sqliteDbFilePath = Paths.get(sqliteDbBasePath, "catma.db");
        return DriverManager.getConnection("jdbc:sqlite:" + sqliteDbFilePath.toRealPath());
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
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS notices (id integer primary key autoincrement, message text, date_created text)");
            statement.executeUpdate("INSERT INTO notices (message, date_created) VALUES ('This is a test notice', '" + Instant.now().toString() + "')");
            ResultSet rs = statement.executeQuery("SELECT name FROM sqlite_schema WHERE type='table' ORDER BY name;");
            while (rs.next()) {
                logger.log(Level.INFO, "name = " + rs.getString("name"));
            }
        }
        catch (IOException | SQLException e) {
            throw new IllegalStateException("Failed to initialise Database", e);
        }
    }

    public ArrayList<SqliteModel.Notice> getNotices() throws Exception {
        try (
                Connection connection = getConnection();
        ) {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("SELECT * FROM notices");
            ArrayList<SqliteModel.Notice> notices = new ArrayList<>();
            while (rs.next()) {
                notices.add(new SqliteModel.Notice(rs.getString("message"), Instant.parse(rs.getString("date_created"))));
            }
            return notices;
        }
        catch (IOException | SQLException e) {
            throw new Exception("Couldn't fetch notices", e);
        }
    }
}
