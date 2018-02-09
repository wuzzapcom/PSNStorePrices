package com.wuzzapcom.psstoreprices

import com.wuzzapcom.psstoreprices.bot.TelegramBot
import com.wuzzapcom.psstoreprices.updater.Updater
import kotlin.concurrent.thread

fun main(args: Array<String>){
    startUpdater()
    TelegramBot.createBot(args[0])
}

fun startUpdater(){
    thread(start = true){
        while(true){
            Updater.tick()
            Thread.sleep(1000*60)
        }
    }
}
