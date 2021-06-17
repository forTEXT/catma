package de.catma.servlet;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.sql.DataSource;
import java.sql.*;

public class SqliteInitializerServlet extends HttpServlet {
    private static final String jndiName = "jdbc/sqlite";
    private DataSource dataSource;

    @Override
    public void init() throws ServletException {
        super.init();

        log("SQLite initializing");

        try {
            try {
                dataSource = (DataSource) new InitialContext().lookup("java:comp/env/" + jndiName);
            }
            catch (NamingException e) {
                throw new IllegalStateException(jndiName + " is missing in JNDI!", e);
            }

            try (
                    Connection connection = dataSource.getConnection();
            ) {
                Statement statement = connection.createStatement();
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS notices (id integer primary key autoincrement, message text, date_created text)");
                ResultSet rs = statement.executeQuery("SELECT name FROM sqlite_schema WHERE type='table' ORDER BY name;");
                while (rs.next()) {
                    log("name = " + rs.getString("name"));
                }
            }
        }
        catch (SQLException e) {
            throw new ServletException(e);
        }
    }
}
