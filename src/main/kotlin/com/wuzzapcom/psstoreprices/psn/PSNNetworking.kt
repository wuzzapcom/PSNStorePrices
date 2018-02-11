package com.wuzzapcom.psstoreprices.psn

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

class PSNNetworking {
    private val urlRoot = "https://store.playstation.com/chihiro-api"
    private val ruStore = "Ru/ru"
    private val apiVersion = "19"
    private val fetchSize = "999999"
    private val logger = Logger.getLogger("PSNNetworkingLogger")
    companion object {
        val PRIMARY_PLATFORM = "ps4"
    }

    private val ContentTypesShouldBeMissed = arrayOf("Тема", "Аватар", "Игровое видео")

    public fun search(forName: String): Array<PSGame>{

        val encodedGameTitle = URLEncoder.encode(normalizeString(forName), "UTF-8")

        val searchRequestURL = URL("$urlRoot/bucket-search/$ruStore/$apiVersion/$encodedGameTitle?size=$fetchSize&start=0")
        logger.log(Level.INFO, "Created URL: $searchRequestURL")

        val baseObject = jacksonObjectMapper().readTree(searchRequestURL)
        val categories = baseObject["categories"]
        val games = categories["games"]
        val links = games["links"]

        val searchResults = ArrayList<PSGame>()
        links.mapTo(searchResults) { parsePSGame(it) }

        searchResults.removeIf { game -> ContentTypesShouldBeMissed.contains(game.contentType) }

        return searchResults.toTypedArray()
    }

    public fun requestInfo(byId: String): PSGame {

        val encodedId = URLEncoder.encode(byId, "UTF-8")

        val requestInfoURL = URL("$urlRoot/viewfinder/$ruStore/$apiVersion/$encodedId?size=$fetchSize")
        logger.log(Level.INFO, "Created URL: $requestInfoURL")

        return parsePSGame(jacksonObjectMapper().readTree(requestInfoURL))
    }

    private fun parsePSGame(fromJSON: JsonNode): PSGame {

        val id = fromJSON["id"].asText()
        val name = fromJSON["short_name"].asText()

        val platforms = ArrayList<String>()
        fromJSON["playable_platform"]?.forEach { platform -> platforms.add(platform.asText()) }
        val platform =
                if (platforms.contains(PRIMARY_PLATFORM))
                    PRIMARY_PLATFORM
                else
                    platforms.first()

        val price =
                if (fromJSON["default_sku"] == null || fromJSON["default_sku"]["price"] == null)
                    0
                else
                    fromJSON["default_sku"]["price"].asInt()

        val contentType =
                if (fromJSON["game_contentType"] == null)
                    ""
                else
                    fromJSON["game_contentType"].asText()

        val salePrice =
                if (fromJSON["default_sku"] == null || fromJSON["default_sku"]["rewards"].asSequence().count() == 0){
                    null
                }else{
                    fromJSON["default_sku"]["rewards"].asIterable().first()["price"].asInt()
                }
        val saleEnd =
                if (fromJSON["default_sku"] == null
                        || fromJSON["default_sku"]["rewards"] == null
                        || fromJSON["default_sku"]["rewards"].asSequence().count() == 0
                        || fromJSON["default_sku"]["rewards"].asIterable().first()["end_date"] == null){
                    null
                }else{
                    LocalDate.parse(fromJSON["default_sku"]["rewards"].asIterable().first()["end_date"].asText(), DateTimeFormatter.ISO_DATE_TIME)
                }

        return PSGame(
                id = id,
                name = name,
                platform = platform,
                price = price,
                contentType = contentType,
                salePrice = salePrice,
                saleEnd = saleEnd
        )
    }

    private fun normalizeString(input: String): String{
        input.replace("\\", "")
        input.replace("/", "")
        return input
    }
}