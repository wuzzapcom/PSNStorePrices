package com.wuzzapcom.psstoreprices.psn

import java.time.LocalDate
import java.util.*

data class PSGame(
        val id: String,
        val name: String,
        val platform: String,
        val price: Int,
        val contentType: String,
        val salePrice: Int? = null,
        val saleEnd: LocalDate? = null
) {

    companion object {
        const val CONTENT_TYPE_EXPANSION = "Дополнение"
        const val CONTENT_TYPE_LEVEL = "Уровень"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PSGame

        if (id != other.id) return false
        if (name != other.name) return false
        if (price != other.price) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + price
        return result
    }

    override fun toString(): String {
        var result = name

        if (platform != PSNNetworking.PRIMARY_PLATFORM){
            result += ". Платформа: $platform"
        }

        if (contentType != "")
            result += ". $contentType"

        if (salePrice != null){
            result += ". Скидка ${salePrice / 100} до ${saleEnd.toString()}"
        }
        result += ". Полная цена: ${price / 100}"

        return result
    }
}