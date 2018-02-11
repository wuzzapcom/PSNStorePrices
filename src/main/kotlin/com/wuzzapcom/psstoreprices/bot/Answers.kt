package com.wuzzapcom.psstoreprices.bot

class Answers {
    companion object {
        const val WRONG_MESSAGE_IN_CURRENT_STATE_MESSAGE = "Неверный ввод"
        const val WAIT_FOR_GAME_NAME_MESSAGE = "Введите название игры"
        const val EXPECTED_INT_GOT_STRING = "Вы ввели не число, попробуйте еще раз"
        const val WRONG_NUMBER = "Вы ввели некорректное число, попробуйте еще раз"
        const val WAIT_ACTION = "Что вы хотите сделать: уведомлять о скидках(введите \"уведомлять\"), узнать текущую цену(введите \"посмотреть\") или не делать ничего(введите \"стоп\")."
        const val WRONG_ACTION = "Вы ввели неверное действие, попробуйте еще раз"
        const val WAIT_FOR_NUMBER_INSTRUCTION = "Введите номер нужной игры чтобы посмотреть дальнейшие действия. Введите \"стоп\", чтобы остановить."
        const val FINISHED_MESSAGE = "Завершено."
        const val ERROR_MESSAGE = "Упс, у нас случилась ошибка. Попробуйте еще раз чуть позже."
        const val UPDATE_PRICE_LOWERED_FORMATTED = "Привет, хорошие новости! Цена на игру %s упала, %d вместо %d."
        const val UPDATE_PRICE_INCREASED_FORMATTED = "Привет, плохие новости. Почему-то цена на игру %s поднялась, %d вместо %d."
        const val SALE_STARTED_FORMATTED = "Привет, хорошие новости! Началась распродажа на игру %s, новая цена %d вместо %d. Она продлится до %s."
        const val SALE_FINISHED_FORMATTED = "Привет, плохие новости. Распродажа на игру %s закончилась, цена вернулась к %d."
        const val HELP_MESSAGE = "Привет! Этот бот предназначен для тех, кто не хочет пропустить скидку на ожидаемую игру. " +
                "Пользоваться им просто - воспользуйтесь командой /search, найдите нужную вам игру и бот будет " +
                "присылать вам уведомления о всех изменениях цен на эту игру. /list покажет вам список тех игр, " +
                "за которыми вы следите, а командой /delete вы сможете удалить игру," +
                "если уже ее купили."
        const val NO_GAMES_MESSAGE = "Вы не добавили пока ни одну игру."
        const val DELETE_GAME_LIST_MESSAGE = "Выберите номер игры, которую следует удалить. Введите \"стоп\", чтобы остановить."
        const val DELETE_GAME_NO_GAMES = "У вас нет ни одной игры."

    }
}