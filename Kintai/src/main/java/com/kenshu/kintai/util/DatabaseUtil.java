package com.kenshu.kintai.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseUtil {

    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream input = DatabaseUtil.class.getClassLoader().getResourceAsStream("database.properties")) {
            if (input == null) {
                System.err.println("Sorry, unable to find database.properties");
                // エラーハンドリングを適切に行う
            }
            PROPERTIES.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
            // エラーハンドリングを適切に行う
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                PROPERTIES.getProperty("db.url"),
                PROPERTIES.getProperty("db.user"),
                PROPERTIES.getProperty("db.password")
        );
    }
}
