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
        val i = response.indexOf("\n30) ")
        if (i == -1)
            result.add(response)
        else{
            result.add(response.substring(0, i))
            val j = response.indexOf("\n60) ")
            if (j == -1)
                result.add(response.substring(i))
            else{
                result.add(response.substring(i, j))
                result.add(response.substring(j))
            }
        }
        return result.toTypedArray()
    }

    private fun send(msg: String, to: Message){
        logger.log(Level.INFO, msg)

        val messageSender = SendMessage()
        messageSender.chatId = to.chatId.toString()
        messageSender.text = msg
        sendMessage(messageSender)
    }
}