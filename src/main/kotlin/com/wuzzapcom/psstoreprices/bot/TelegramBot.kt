package com.wuzzapcom.psstoreprices.bot

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.telegram.telegrambots.ApiContextInitializer
import org.telegram.telegrambots.TelegramBotsApi
import org.telegram.telegrambots.api.methods.send.SendMessage
import org.telegram.telegrambots.api.objects.Message
import org.telegram.telegrambots.api.objects.Update
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.exceptions.TelegramApiException
import java.net.URL
import java.net.URLEncoder
import java.util.logging.Level
import java.util.logging.Logger

class TelegramBot: TelegramLongPollingBot {
    private val logger = Logger.getLogger("TelegramBotLogger")

    /*
        This initiation required, because Telegram API requires ApiContextInitializer.init().
        May be there is better solution.
     */


    private val idToModel = HashMap<Int, Model>()

    companion object {
        private var token: String = ""

        fun createBot(t: String): TelegramBot{
            token = t
            ApiContextInitializer.init()
            return TelegramBot()
        }

        fun sendSingleMessage(withText: String, to: Int){
            val url = URL("https://api.telegram.org/bot$token/sendMessage?chat_id=$to&text=${URLEncoder.encode(withText, "UTF-8")}")
            jacksonObjectMapper().readTree(url)
        }
    }

    private constructor(): super(){
        val api = TelegramBotsApi()
        try {
            api.registerBot(this)
        }
        catch (e: TelegramApiException){
            e.printStackTrace()
        }
    }

    override fun getBotToken(): String {
        return token
    }

    override fun getBotUsername(): String {
        return "PSNSalesNotifierBot"
    }

    override fun onUpdateReceived(update: Update?) {
        if (update == null)
            return

        if (!idToModel.containsKey(update.message.from.id)){
            idToModel[update.message.from.id] = Model()
        }

        val response = idToModel[update.message.from.id]!!.respond(update.message)
        splitResponse(response).forEach { r -> send(r, update.message) }
    }

    private fun splitResponse(response: String): Array<String>{
        val result = ArrayList<String>()
        var numberOfStrings = 1
        val numberOfStringsInMessage = 30
        response.forEach { symbol ->
            if (symbol == '\n')
                numberOfStrings++
        }
        if (numberOfStrings / numberOfStringsInMessage > 1){
            val numberOfMessages = numberOfStrings / numberOfStringsInMessage

            var mess = ""
            var currentNumberOfStringsInResult = 0
            var numberOfCurrentSymbol = 0

            for (i in 0..numberOfMessages){
                while(
                        currentNumberOfStringsInResult != numberOfStringsInMessage
                        && numberOfCurrentSymbol < response.length - 1
                ){

                    mess += response[numberOfCurrentSymbol]
                    numberOfCurrentSymbol++
                    if (response[numberOfCurrentSymbol] == '\n')
                        currentNumberOfStringsInResult++
                }
                result.add(mess)
                mess = ""
                currentNumberOfStringsInResult = 0
            }
            return result.toTypedArray()
        }
        else{
            return arrayOf(response)
        }
    }

    private fun send(msg: String, to: Message){
        logger.log(Level.INFO, "Thread with user ${to.chatId}.\n Source message = ${to.text}.\n Answer = $msg")

        val messageSender = SendMessage()
        messageSender.chatId = to.chatId.toString()
        messageSender.text = msg
        sendMessage(messageSender)
    }
}