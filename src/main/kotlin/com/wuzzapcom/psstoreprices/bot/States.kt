package com.wuzzapcom.psstoreprices.bot


enum class States(val command: String? = null){
    NO_STATE(),
    SEARCH_GAME_WAIT_NAME_STATE("/search"),
    SEARCH_GAME_WAIT_NAME_NO_EXPANSIONS_STATE("/search_no_expansions"),
    SEARCH_GAME_WAIT_NUMBER_STATE(),
    SEARCH_GAME_WAIT_ACTION(),
    DELETE_GAME_STATE("/delete"),
    LIST_NOTIFICATION_GAMES("/list")
}