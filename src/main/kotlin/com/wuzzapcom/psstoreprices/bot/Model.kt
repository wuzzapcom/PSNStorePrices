package com.wuzzapcom.psstoreprices.bot

import com.wuzzapcom.psstoreprices.database.Database
import com.wuzzapcom.psstoreprices.psn.PSGame
import com.wuzzapcom.psstoreprices.psn.PSNNetworking
import org.telegram.telegrambots.api.objects.Message
import java.util.logging.Level
import java.util.logging.Logger

class Model {

    private val logger = Logger.getLogger("ModelLogger")

    private val NOTIFY_ACTION = "уведомлять"
    private val SHOW_ACTION = "посмотреть"
    private val NOTHING_ACTION = "стоп"

    private var currentState = States.NO_STATE
    private var foundGames: Array<PSGame>? = null
    private var selectedGame: PSGame? = null

    fun respond(on: Message): String{

        val userID = on.from.id
        val message = on.text

        if (message == "/help"){
            return Answers.HELP_MESSAGE
        }

        if (currentState == States.NO_STATE) {
            val arrayOfStates = States.values()
            for (state in arrayOfStates) {
                if (state.command.equals( message)){
                    return when(state){
                        States.SEARCH_GAME_WAIT_NAME_STATE -> {
                            logger.log(
                                    Level.INFO,
                                    "Thread with user $userID.\nCurrent state: ${currentState.name}.\n" +
                                            "Selected ${state.name}(${state.command}) with message = $message"
                            )
                            currentState = States.SEARCH_GAME_WAIT_NAME_STATE
                            Answers.WAIT_FOR_GAME_NAME_MESSAGE
                        }
                        States.DELETE_GAME_STATE -> {
                            logger.log(
                                    Level.INFO,
                                    "Thread with user $userID.\nCurrent state: ${currentState.name}.\n" +
                                            "Selected ${state.name}(${state.command}) with message = $message"
                            )
                            currentState = States.DELETE_GAME_STATE
                            Answers.WAIT_FOR_GAME_NAME_MESSAGE
                        }
                        States.LIST_NOTIFICATION_GAMES -> {
                            logger.log(
                                    Level.INFO,
                                    "Thread with user $userID.\nCurrent state: ${currentState.name}.\n" +
                                            "Selected ${state.name}(${state.command}) with message = $message"
                            )
                            var result = ""
                            val db = Database()
                            db.getGamesList(userID).forEach {game ->
                                result += "$game\n"
                            }
                            db.close()
                            if (result == "")
                                Answers.NO_GAMES_MESSAGE
                            else
                                result
                        }
                        else -> {
                            logger.log(
                                    Level.INFO,
                                    "Thread with user $userID.\nCurrent state: ${currentState.name}.\n" +
                                            "Selected ${state.name}(${state.command}) with message = $message"
                            )
                            Answers.WRONG_MESSAGE_IN_CURRENT_STATE_MESSAGE
                        }
                    }
                }
            }
        }
        else if (currentState == States.SEARCH_GAME_WAIT_NAME_STATE){
            logger.log(
                    Level.INFO,
                    "Thread with user $userID.\nCurrent state: ${currentState.name} with message = $message"
            )
            currentState = States.SEARCH_GAME_WAIT_NUMBER_STATE
            val psn = PSNNetworking()
            var result = ""
            try {
                foundGames = psn.search(message)
            }catch(e: Exception){
                e.printStackTrace()
                currentState = States.NO_STATE
            }

            for ((index, game) in foundGames!!.withIndex()) {

                result += "${index + 1}) $game\n"

            }

            result += Answers.WAIT_FOR_NUMBER_INSTRUCTION

            return result
        }
        else if (currentState == States.SEARCH_GAME_WAIT_NUMBER_STATE){
            logger.log(
                    Level.INFO,
                    "Thread with user $userID.\nCurrent state: ${currentState.name} with message = $message"
            )
            if (message == NOTHING_ACTION){
                currentState = States.NO_STATE
                return Answers.FINISHED_MESSAGE
            }

            val number = message.toIntOrNull() ?: return Answers.EXPECTED_INT_GOT_STRING

            if (number > foundGames!!.size || number < 1)
                return Answers.WRONG_NUMBER

            selectedGame = foundGames!![number - 1]
            foundGames = null

            currentState = States.SEARCH_GAME_WAIT_ACTION
            return Answers.WAIT_ACTION
        }
        else if (currentState == States.SEARCH_GAME_WAIT_ACTION){
            logger.log(
                    Level.INFO,
                    "Thread with user $userID.\nCurrent state: ${currentState.name} with message = $message"
            )
            if (message != NOTIFY_ACTION && message != SHOW_ACTION && message != NOTHING_ACTION)
                return Answers.WRONG_ACTION

            currentState = States.NO_STATE

            if (message == NOTIFY_ACTION){
                return createNotification(userID, selectedGame!!)
            }
            else if (message == SHOW_ACTION){
                return selectedGame.toString()
            }
            else {
                return Answers.FINISHED_MESSAGE
            }
        }
        else if (currentState == States.DELETE_GAME_STATE){
            logger.log(
                    Level.INFO,
                    "Thread with user $userID.\nCurrent state: ${currentState.name} with message = $message"
            )
            var database: Database? = null
            try{
                database = Database()
                val game = database.getGameByName(message)
                if (game != null)
                    database.deleteNotification(userID, game.id)
            }catch(e: Exception){
                return Answers.ERROR_MESSAGE
            }
            finally {
                database?.close()
                currentState = States.NO_STATE
            }
            return Answers.FINISHED_MESSAGE
        }
        return Answers.WRONG_MESSAGE_IN_CURRENT_STATE_MESSAGE
    }

    private fun createNotification(forUser: Int, withGame: PSGame): String{
        var database: Database? = null
        try{
            database = Database()
            database.insertUser(forUser)
            database.insertGame(withGame)
            database.insertNotification(forUser, withGame.id)
        }catch(e: Exception){
            return Answers.ERROR_MESSAGE
        }
        finally {
            database?.close()
        }
        return Answers.FINISHED_MESSAGE
    }

    companion object {
        fun responseMessageForUpdatePrice(gameName: String, new: Int, old: Int): String{
            return if (new < old){
                String.format(Answers.UPDATE_PRICE_LOWERED_FORMATTED, gameName, new, old)
            }
            else{
                String.format(Answers.UPDATE_PRICE_INCREASED_FORMATTED, gameName, new, old)
            }
        }

        fun responseMessageForSaleFinished(gameName: String, price: Int): String{
            return String.format(Answers.SALE_FINISHED_FORMATTED, gameName, price)
        }

        fun responseMessageForSaleStart(gameName: String, new: Int, old: Int, endDate: String): String{
            return String.format(Answers.SALE_STARTED_FORMATTED, gameName, new, old, endDate)
        }
    }

}

enum class States(val command: String? = null){
    NO_STATE(),
    SEARCH_GAME_WAIT_NAME_STATE("/search"),
    SEARCH_GAME_WAIT_NUMBER_STATE(),
    SEARCH_GAME_WAIT_ACTION(),
    DELETE_GAME_STATE("/delete"),
    LIST_NOTIFICATION_GAMES("/list")
}