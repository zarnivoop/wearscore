package com.squashscore.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant

/**
 * Custom serializer for java.time.Instant — stores as epoch millis (Long).
 * Required because kotlinx.serialization has no built-in Instant serializer.
 */
object InstantSerializer : KSerializer<Instant> {
    override val descriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeLong(value.toEpochMilli())
    }

    override fun deserialize(decoder: Decoder): Instant {
        return Instant.ofEpochMilli(decoder.decodeLong())
    }
}
