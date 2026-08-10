package no.iktdev.kammich.util

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val gson = GsonBuilder()
    .registerTypeAdapter(Instant::class.java, InstantAdapter())
    // hvis du fortsatt har LocalDateTime et sted:
    .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
    .setPrettyPrinting()
    .create()

class InstantAdapter : JsonSerializer<Instant>, JsonDeserializer<Instant> {
    override fun serialize(
        src: Instant,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement =
        JsonPrimitive(src.toString()) // ISO-8601, UTC

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Instant =
        Instant.parse(json.asString)
}

class LocalDateTimeAdapter : JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    override fun serialize(
        src: LocalDateTime,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement =
        JsonPrimitive(src.format(formatter))

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): LocalDateTime =
        LocalDateTime.parse(json.asString, formatter)
}