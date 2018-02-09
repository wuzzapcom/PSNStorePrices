package com.wuzzapcom.psstoreprices.updater

import com.wuzzapcom.psstoreprices.bot.Model
import com.wuzzapcom.psstoreprices.bot.TelegramBot
import com.wuzzapcom.psstoreprices.database.Database
import com.wuzzapcom.psstoreprices.psn.PSNNetworking
import java.util.logging.Level
import java.util.logging.Logger

class Updater {

    companion object {
        fun tick(){
            val logger = Logger.getLogger("UpdaterLogger")
            try {
                logger.log(Level.INFO, "tick")
                val database = Database()
                val psn = PSNNetworking()

                val psnIds = database.getAllPSNID()

                for(id in psnIds){
                    val updatedGame = psn.requestInfo(id)
                    val oldGame = database.getGame(id) ?: continue

                    if (updatedGame.price != oldGame.price){
                        database.update(id, updatedGame.price)
                        val message = Model.responseMessageForUpdatePrice(
                                updatedGame.name,
                                updatedGame.price,
                                oldGame.price
                        )
                        database.getUsers(id).forEach {userID ->
                            TelegramBot.sendSingleMessage(
                                    message,
                                    userID
                            )
                        }
                    }
                    logger.log(Level.INFO, "${updatedGame.salePrice}, ${oldGame.salePrice}, ${oldGame.saleEnd}")
                    if (updatedGame.salePrice != oldGame.salePrice){
                        if (updatedGame.salePrice != null && updatedGame.saleEnd != null) {
                            database.update(
                                    id,
                                    updatedGame.salePrice,
                                    updatedGame.saleEnd.toString()
                            )
                            val message = Model.responseMessageForSaleStart(
                                    updatedGame.name,
                                    updatedGame.price,
                                    oldGame.price,
                                    updatedGame.saleEnd.toString()
                            )
                            database.getUsers(id).forEach {userID ->
                                TelegramBot.sendSingleMessage(
                                        message,
                                        userID
                                )
                            }
                        }
                        else{
                            database.cleanSale(id)
                            val message = Model.responseMessageForSaleFinished(
                                    updatedGame.name,
                                    updatedGame.price
                            )
                            database.getUsers(id).forEach {userID ->
                                TelegramBot.sendSingleMessage(
                                        message,
                                        userID
                                )
                            }
                        }
                    }
                }
                database.close()
            }
            catch (e: Exception){
                e.printStackTrace()
            }
        }
    }


}