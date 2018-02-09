package com.wuzzapcom.psstoreprices.database

import java.sql.DriverManager
import java.sql.SQLException
import java.util.logging.Level
import java.util.logging.Logger

internal class DatabaseCreator {

    companion object {

        internal fun create(){
            createUsersTable()
            createPlatformsTable()
            createGamesTable()
            createNotificationsTable()
        }

        private fun createDatabase() {

            try {
                val connection = DriverManager.getConnection(Database.databaseURL)
                if (connection != null) {
                    val metaData = connection.metaData
                }

            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }

        private fun createUsersTable() {
            val command = "CREATE TABLE IF NOT EXISTS USERS (\n" +
                    "UserID INTEGER PRIMARY KEY\n" +
                    ")"
            executeCreateTable(command)
        }

        private fun createGamesTable() {
            val command = "CREATE TABLE IF NOT EXISTS GAMES (\n" +
                    "PSN_ID TEXT PRIMARY KEY,\n" +
                    "Name TEXT UNIQUE,\n" +
                    "Platform INTEGER NOT NULL,\n" +
                    "Price INTEGER NOT NULL,\n" +
                    "ContentType TEXT NOT NULL,\n" +
                    "SalePrice INTEGER,\n" +
                    "SaleEnd TEXT,\n" +
                    "FOREIGN KEY (Platform) REFERENCES PLATFORMS(PlatformID)\n" +
                    ")"
            executeCreateTable(command)
        }

        private fun createNotificationsTable() {
            val command = "CREATE TABLE IF NOT EXISTS NOTIFICATIONS (\n" +
                    "UserID INTEGER,\n" +
                    "PSN_ID TEXT,\n" +
                    "PRIMARY KEY (UserID, PSN_ID),\n" +
                    "FOREIGN KEY (UserID) REFERENCES USERS(UserID),\n" +
                    "FOREIGN KEY (PSN_ID) REFERENCES GAMES(PSN_ID)\n" +
                    ")"
            executeCreateTable(command)
        }

        private fun createPlatformsTable() {
            val command = "CREATE TABLE IF NOT EXISTS PLATFORMS (\n" +
                    "PlatformID INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                    "PlatformName TEXT UNIQUE\n" +
                    ")"
            executeCreateTable(command)
        }

        private fun executeCreateTable(createTableCommand: String) {
            try {
                val connection = DriverManager.getConnection(Database.databaseURL)
                val statement = connection.createStatement()
                statement.execute(createTableCommand)
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }
    }
}