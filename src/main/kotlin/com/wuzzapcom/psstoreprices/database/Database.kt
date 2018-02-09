package com.wuzzapcom.psstoreprices.database

import com.wuzzapcom.psstoreprices.psn.PSGame
import java.sql.DriverManager
import java.sql.SQLException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.logging.Logger

class Database {

    private val logger = Logger.getLogger("DatabaseLogger")
    companion object {
        internal val databaseURL = "jdbc:sqlite:psnprices.sqlite3"
    }
    private val connection = DriverManager.getConnection(databaseURL)

    init{
        DatabaseCreator.create()
    }

    fun insertUser(userID: Int){
        val command = "INSERT INTO USERS(UserID) VALUES (?)"
        try{
            val statement = connection.prepareStatement(command)
            statement.setInt(1, userID)
            statement.executeUpdate()
        }
        catch (e: SQLException){
            e.printStackTrace()
        }
    }

    fun insertGame(game: PSGame){
        if (game.salePrice != null) {
            val command = "INSERT INTO GAMES(PSN_ID, Name, Platform, Price, ContentType, SalePrice, SaleEnd) VALUES(?, ?, ?, ?, ?, ?, ?)"
            try {
                val statement = connection.prepareStatement(command)
                statement.setString(1, game.id)
                statement.setString(2, game.name)
                statement.setInt(3, findPlatformID(game.platform)!!)
                statement.setInt(4, game.price)
                statement.setString(5, game.contentType)
                statement.setInt(6, game.salePrice)
                statement.setString(7, game.saleEnd.toString())
                statement.executeUpdate()
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }
        else{
            val command = "INSERT INTO GAMES(PSN_ID, Name, Platform, Price, ContentType) VALUES(?, ?, ?, ?, ?)"
            try {
                val statement = connection.prepareStatement(command)
                statement.setString(1, game.id)
                statement.setString(2, game.name)
                statement.setInt(3, findPlatformID(game.platform)!!)
                statement.setInt(4, game.price)
                statement.setString(5, game.contentType)
                statement.executeUpdate()
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }
    }

    fun insertNotification(userID: Int, PSN_ID: String){
        val command = "INSERT INTO NOTIFICATIONS(UserID, PSN_ID) VALUES (?, ?)"
        try {
            val statement = connection.prepareStatement(command)
            statement.setInt(1, userID)
            statement.setString(2, PSN_ID)
            statement.executeUpdate()
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    }

    fun insertPlatform(withName: String){
        val command = "INSERT INTO PLATFORMS(PlatformName) VALUES (?)"
        try {
            val statement = connection.prepareStatement(command)
            statement.setString(1, withName)
            statement.executeUpdate()
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    }

    fun update(gameWithPSN_ID: String, withNewPrice: Int){
        val command = "UPDATE GAMES SET Price = ? WHERE PSN_ID = ?"
        try {
            val statement = connection.prepareStatement(command)
            statement.setInt(1, withNewPrice)
            statement.setString(2, gameWithPSN_ID)
            statement.executeUpdate()
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    }

    fun update(gameWithPSN_ID: String, withSalePrice: Int, until: String){
        val command = "UPDATE GAMES SET SalePrice = ?, SaleEnd = ? WHERE PSN_ID = ?"
        try {
            val statement = connection.prepareStatement(command)
            statement.setInt(1, withSalePrice)
            statement.setString(2, until)
            statement.setString(3, gameWithPSN_ID)
            statement.executeUpdate()
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    }

    fun cleanFinishedSales(){
        val command = "SELECT PSN_ID, SaleEnd FROM GAMES"
        val currentDate = LocalDate.now()
        try{
            val statement = connection.createStatement()
            val resultSet = statement.executeQuery(command)
            while (resultSet.next()){
                val saleEndString = resultSet.getString("SaleEnd") ?: continue
                val saleEnd = LocalDate.parse(saleEndString, DateTimeFormatter.ISO_DATE_TIME)
                if (saleEnd.isAfter(currentDate)){
                    cleanSale(
                            resultSet.getString("PSN_ID")
                    )
                }
            }
        }
        catch (e: SQLException){
            e.printStackTrace()
        }
    }

    fun getGames(forUserWithID: Int): Array<PSGame>{
        val games = ArrayList<PSGame>()
        val command = "SELECT * FROM  NOTIFICATIONS JOIN GAMES G ON NOTIFICATIONS.GameID = G.GameID WHERE UserID = ?"
        try{
            val statement = connection.prepareStatement(command)
            statement.setInt(1, forUserWithID)
            val resultSet = statement.executeQuery()
            while (resultSet.next()){
                games.add(
                        PSGame(
                                resultSet.getString("PSN_ID"),
                                resultSet.getString("Name"),
                                findPlatform(
                                        resultSet.getInt("Platform")
                                )!!,
                                resultSet.getInt("Price"),
                                resultSet.getString("ContentType"),
                                resultSet.getInt("SalePrice"),
                                inlineStringToLocalDate(
                                        resultSet.getString("SaleEnd")
                                )
                        )
                )
            }
        }
        catch (e: SQLException){
            e.printStackTrace()
        }
        return games.toTypedArray()
    }

    fun getAllPSNID(): Array<String>{
        val command = "SELECT PSN_ID FROM GAMES"
        val result = ArrayList<String>()
        try{
            val statement = connection.createStatement()
            val resultSet = statement.executeQuery(command)

            while(resultSet.next()){
                result.add(
                        resultSet.getString("PSN_ID")
                )
            }
        }
        catch (e: SQLException){
            e.printStackTrace()
        }
        return result.toTypedArray()
    }

    fun getGame(byPSN_ID: String): PSGame?{
        val command = "SELECT * FROM GAMES WHERE PSN_ID = ?"
        try{
            val statement = connection.prepareStatement(command)
            statement.setString(1, byPSN_ID)
            val resultSet = statement.executeQuery()
            while (resultSet.next()){
                var salePrice: Int? = resultSet.getInt("SalePrice")
                if (resultSet.wasNull()){
                    salePrice = null
                }
                return PSGame(
                        resultSet.getString("PSN_ID"),
                        resultSet.getString("Name"),
                        findPlatform(
                                resultSet.getInt("Platform")
                        )!!,
                        resultSet.getInt("Price"),
                        resultSet.getString("ContentType"),
                        salePrice,
                        inlineStringToLocalDate(
                                resultSet.getString("SaleEnd")
                        )
                )
            }
        }
        catch (e: SQLException){
            e.printStackTrace()
        }
        return null
    }

    fun getGameByName(name: String): PSGame?{
        val command = "SELECT * FROM GAMES WHERE Name = ?"
        try{
            val statement = connection.prepareStatement(command)
            statement.setString(1, name)
            val resultSet = statement.executeQuery()
            while (resultSet.next()){
                var salePrice: Int? = resultSet.getInt("SalePrice")
                if (resultSet.wasNull()){
                    salePrice = null
                }
                return PSGame(
                        resultSet.getString("PSN_ID"),
                        resultSet.getString("Name"),
                        findPlatform(
                                resultSet.getInt("Platform")
                        )!!,
                        resultSet.getInt("Price"),
                        resultSet.getString("ContentType"),
                        salePrice,
                        inlineStringToLocalDate(
                                resultSet.getString("SaleEnd")
                        )
                )
            }
        }
        catch (e: SQLException){
            e.printStackTrace()
        }
        return null
    }

    fun getUsers(notifiedByPSN_ID: String): Array<Int>{
        val result = ArrayList<Int>()
        val command = "SELECT UserID FROM NOTIFICATIONS WHERE PSN_ID = ?"
        try{
            val statement = connection.prepareStatement(command)
            statement.setString(1, notifiedByPSN_ID)
            val resultSet = statement.executeQuery()
            while (resultSet.next()){
                result.add(resultSet.getInt("UserID"))
            }
        }
        catch (e: SQLException){
            e.printStackTrace()
        }
        return result.toTypedArray()
    }

    fun getGamesList(forUser: Int): Array<PSGame>{
        val result = ArrayList<PSGame>()
        val command = "SELECT * FROM NOTIFICATIONS JOIN GAMES G ON NOTIFICATIONS.PSN_ID = G.PSN_ID WHERE UserID = ?"
        try{
            val statement = connection.prepareStatement(command)
            statement.setInt(1, forUser)
            val resultSet = statement.executeQuery()
            while (resultSet.next()){
                var salePrice: Int? = resultSet.getInt("SalePrice")
                if (resultSet.wasNull()){
                    salePrice = null
                }
                result.add(PSGame(
                        resultSet.getString("PSN_ID"),
                        resultSet.getString("Name"),
                        findPlatform(
                                resultSet.getInt("Platform")
                        )!!,
                        resultSet.getInt("Price"),
                        resultSet.getString("ContentType"),
                        salePrice,
                        inlineStringToLocalDate(
                                resultSet.getString("SaleEnd")
                        )
                )
                )
            }
        }
        catch (e: SQLException){
            e.printStackTrace()
        }
        return result.toTypedArray()
    }

    fun deleteNotification(forUser: Int, forGameWithPSN_ID: String){
        val command = "DELETE FROM NOTIFICATIONS WHERE UserID = ? AND PSN_ID = ?"
        try {
            val statement = connection.prepareStatement(command)
            statement.setInt(1, forUser)
            statement.setString(2, forGameWithPSN_ID)
            statement.executeUpdate()
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    }

    fun close(){
        connection.close()
    }

    fun cleanSale(forGameWithPSN_ID: String){
        val command = "UPDATE GAMES SET SalePrice = NULL, SaleEnd = NULL WHERE PSN_ID = ?"
        try {
            val statement = connection.prepareStatement(command)
            statement.setString(1, forGameWithPSN_ID)
            statement.executeUpdate()
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    }

    private fun findPlatformID(byPlatform: String): Int?{
        val command = "SELECT PlatformID FROM PLATFORMS WHERE PlatformName = ?"
        try{
            val statement = connection.prepareStatement(command)
            statement.setString(1, byPlatform)
            val resultSet = statement.executeQuery()
            return resultSet.getInt("PlatformID")
        }
        catch (e: SQLException){
            e.printStackTrace()
            insertPlatform(byPlatform)
            return findPlatformID(byPlatform)
        }
        return null
    }

    private fun findGameID(byPSN_ID: String): Int?{
        val command = "SELECT GameID FROM GAMES WHERE PSN_ID = ?"
        try{
            val statement = connection.prepareStatement(command)
            statement.setString(1, byPSN_ID)
            val resultSet = statement.executeQuery()
            return resultSet.getInt("GameID")
        }
        catch (e: SQLException){
            e.printStackTrace()
        }
        return null
    }

    private fun findPlatform(byID: Int): String?{
        val command = "SELECT PlatformName FROM PLATFORMS WHERE PlatformID = ?"
        try{
            val statement = connection.prepareStatement(command)
            statement.setInt(1, byID)
            val resultSet = statement.executeQuery()
            return resultSet.getString("PlatformName")
        }
        catch (e: SQLException){
            e.printStackTrace()
        }
        return null
    }

    private fun inlineStringToLocalDate(date: String?): LocalDate?{
        return if (date == null)
            null
        else
            LocalDate.parse(
                    date,
                    DateTimeFormatter.ISO_DATE
            )
    }


    private fun executeSelect(){
        val selectCommand = "SELECT * FROM t"
        try{
            val statement = connection.createStatement()
            val resultSet = statement.executeQuery(selectCommand)

            while (resultSet.next()){
                println(resultSet.getInt("id").toString() + " " + resultSet.getInt("value").toString())
            }
        }
        catch (e: SQLException){
            e.printStackTrace()
        }
    }
}