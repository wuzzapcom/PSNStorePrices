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

    fun respond(on: Message): String{

        val userID = on.from.id
        val message = on.text

        if (message == "/help"){
            return Answers.HELP_MESSAGE
        }

        return when (currentState) {
            States.NO_STATE ->
                handleNoState(userID, message)
            States.SEARCH_GAME_WAIT_NAME_STATE ->
                handleSearchGameWaitNameState(userID, message)
            States.SEARCH_GAME_WAIT_NUMBER_STATE ->
                handleSearchGameWaitNumberState(userID, message)
            States.SEARCH_GAME_WAIT_ACTION ->
                handleSearchGameWaitAction(userID, message)
            States.DELETE_GAME_STATE ->
                handleDeleteGameState(userID, message)
            else ->
                Answers.WRONG_MESSAGE_IN_CURRENT_STATE_MESSAGE

        }
    }

    private fun handleNoState(userID: Int, message: String): String{
        val arrayOfStates = States.values()
        for (state in arrayOfStates) {
            if (state.command.equals( message)){
                return when(state){
                    States.SEARCH_GAME_WAIT_NAME_STATE -> {
                        logNoStateActions(userID, message, state)
                        currentState = States.SEARCH_GAME_WAIT_NAME_STATE
                        Answers.WAIT_FOR_GAME_NAME_MESSAGE
                    }
                    States.DELETE_GAME_STATE -> {
                        logNoStateActions(userID, message, state)
                        currentState = States.DELETE_GAME_STATE
                        Answers.WAIT_FOR_GAME_NAME_MESSAGE
                    }
                    States.LIST_NOTIFICATION_GAMES -> {
                        logNoStateActions(userID, message, state)
                        getListOfUserGames(userID)
                    }
                    else -> {
                        logger.log(Level.INFO, "Action not found")
                        Answers.WRONG_MESSAGE_IN_CURRENT_STATE_MESSAGE
                    }
                }
            }
        }
        return Answers.WRONG_MESSAGE_IN_CURRENT_STATE_MESSAGE
    }

    private fun handleSearchGameWaitNameState(userID: Int, message: String): String{
        logSelectedState(userID, message)
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

    private fun handleSearchGameWaitNumberState(userID: Int, message: String): String{
        logSelectedState(userID, message)
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

    private fun handleSearchGameWaitAction(userID: Int, message: String): String{
        logSelectedState(userID, message)
        if (message != NOTIFY_ACTION && message != SHOW_ACTION && message != NOTHING_ACTION)
            return Answers.WRONG_ACTION

        currentState = States.NO_STATE

        return when (message) {
            NOTIFY_ACTION -> createNotification(userID, selectedGame!!)
            SHOW_ACTION -> selectedGame.toString()
            else -> Answers.FINISHED_MESSAGE
        }
    }

    private fun handleDeleteGameState(userID: Int, message: String): String{
        logSelectedState(userID, message)
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

    private fun getListOfUserGames(userID: Int, isLinesNumbered: Boolean = false): String{
        var result = ""
        val db = Database()

        var numberOfGameInList = 1

        db.getGamesList(userID).forEach {game ->
            result += if (isLinesNumbered)
                "${numberOfGameInList++}) $game\n"
            else
                "$game\n"
        }
        db.close()
        return if (result == "")
            Answers.NO_GAMES_MESSAGE
        else
            result
    }

    private fun logSelectedState(userID: Int, message: String){
        logger.log(
                Level.INFO,
                "Thread with user $userID.\nCurrent state: ${currentState.name} with message = $message"
        )
    }

    private fun logNoStateActions(userID: Int, message: String, state: States){
        logger.log(
                Level.INFO,
                "Thread with user $userID.\nCurrent state: ${currentState.name}.\n" +
                        "Selected ${state.name}(${state.command}) with message = $message"
        )
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

}