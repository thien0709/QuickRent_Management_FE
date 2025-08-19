package com.bxt.ui.components

import com.google.gson.*
import java.lang.reflect.Type
import java.time.Instant

class InstantAdapter : JsonSerializer<Instant>, JsonDeserializer<Instant> {
    override fun serialize(src: Instant?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src?.toString()) // ISO-8601 chuẩn Z
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Instant {
        return Instant.parse(json!!.asString)
    }
}