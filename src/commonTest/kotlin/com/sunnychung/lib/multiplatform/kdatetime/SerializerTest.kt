package com.sunnychung.lib.multiplatform.kdatetime

import com.sunnychung.lib.multiplatform.kdatetime.serializer.KInstantAsLong
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class SerializerTest {
    @Serializable data class KInstantData(val data: KInstant)
    @Serializable data class KInstantAsLongData(val data: KInstantAsLong)
    @Serializable data class KZonedInstantData(val data: KZonedInstant)
    @Serializable data class KZoneOffsetData(val data: KZoneOffset)
    @Serializable data class KDateData(val data: KDate)
    @Serializable data class KZonedDateTimeData(val data: KZonedDateTime)
    @Serializable data class KDurationData(val data: KDuration)

    @Test
    fun serializeKZonedInstant() {
        val data = KZonedInstantData(KZonedInstant.parseFrom(input = "2023-09-16T19:03:09.674+08:00", formats = listOf(KDateTimeFormat.FULL)))
        val json = Json.encodeToString(data)
        assertEquals("{\"data\":\"2023-09-16T19:03:09.674+08:00\"}", json)
    }

    @Test
    fun deserializeKZonedInstant() {
        val json = "{\"data\":\"2023-09-16T19:03:09.674+08:00\"}"
        val data = Json.decodeFromString<KZonedInstantData>(json)
        assertEquals(1694862189674, data.data.toEpochMilliseconds())
    }

    @Test
    fun serializeKInstant() {
        val data = KInstantData(KInstant.parseFrom(input = "2023-09-16T19:03:09.674+08:00", formats = listOf(KDateTimeFormat.FULL)))
        val json = Json.encodeToString(data)
        assertEquals("{\"data\":\"2023-09-16T11:03:09.674Z\"}", json)
    }

    @Test
    fun deserializeKInstant() {
        val json = "{\"data\":\"2023-09-16T11:03:09.674Z\"}"
        val data = Json.decodeFromString<KInstantData>(json)
        assertEquals(1694862189674, data.data.toEpochMilliseconds())
    }

    @Test
    fun serializeKInstantAsLong() {
        val data = KInstantAsLongData(KInstant.parseFrom(input = "2023-09-16T19:03:09.674+08:00", formats = listOf(KDateTimeFormat.FULL)))
        val json = Json.encodeToString(data)
        assertEquals("{\"data\":1694862189674}", json)
    }

    @Test
    fun deserializeKInstantAsLong() {
        val json = "{\"data\":1694862189674}"
        val data = Json.decodeFromString<KInstantAsLongData>(json)
        assertEquals(1694862189674, data.data.toEpochMilliseconds())
    }

    @Test
    fun serializeKZoneOffset() {
        KZoneOffset(9, 0).let {
            val json = Json.encodeToString(KZoneOffsetData(it))
            assertEquals("{\"data\":\"+09:00\"}", json)
        }
        KZoneOffset(13, 45).let {
            val json = Json.encodeToString(KZoneOffsetData(it))
            assertEquals("{\"data\":\"+13:45\"}", json)
        }
        KZoneOffset(-5, 30).let {
            val json = Json.encodeToString(KZoneOffsetData(it))
            assertEquals("{\"data\":\"-05:30\"}", json)
        }
        KZoneOffset(0, 0).let {
            val json = Json.encodeToString(KZoneOffsetData(it))
            assertEquals("{\"data\":\"Z\"}", json)
        }
    }

    @Test
    fun deserializeKZoneOffset() {
        "{\"data\":\"+09:00\"}".let {
            val data = Json.decodeFromString<KZoneOffsetData>(it)
            assertEquals(9, data.data.hours)
            assertEquals(0, data.data.minutes)
        }
        "{\"data\":\"+13:45\"}".let {
            val data = Json.decodeFromString<KZoneOffsetData>(it)
            assertEquals(13, data.data.hours)
            assertEquals(45, data.data.minutes)
        }
        "{\"data\":\"-05:30\"}".let {
            val data = Json.decodeFromString<KZoneOffsetData>(it)
            assertEquals(-5, data.data.hours)
            assertEquals(30, data.data.minutes)
        }
        "{\"data\":\"Z\"}".let {
            val data = Json.decodeFromString<KZoneOffsetData>(it)
            assertEquals(0, data.data.hours)
            assertEquals(0, data.data.minutes)
        }
    }

    @Test
    fun serializeKDate() {
        val data = KDateData(KDate(2026, 4, 19))
        val json = Json.encodeToString(data)
        assertEquals("{\"data\":{\"year\":2026,\"month\":4,\"day\":19}}", json)
    }

    @Test
    fun deserializeKDate() {
        "{\"data\":{\"year\":2026,\"month\":4,\"day\":19}}".let {
            val data = Json.decodeFromString<KDateData>(it)
            assertEquals(2026, data.data.year)
            assertEquals(4, data.data.month)
            assertEquals(19, data.data.day)
        }
        "{\"data\":{\"year\":1970,\"month\":1,\"day\":1}}".let {
            val data = Json.decodeFromString<KDateData>(it)
            assertEquals(1970, data.data.year)
            assertEquals(1, data.data.month)
            assertEquals(1, data.data.day)
        }
        "{\"data\":{\"year\":2000,\"month\":1,\"day\":1}}".let {
            val data = Json.decodeFromString<KDateData>(it)
            assertEquals(2000, data.data.year)
            assertEquals(1, data.data.month)
            assertEquals(1, data.data.day)
        }
        "{\"data\":{\"year\":1999,\"month\":12,\"day\":31}}".let {
            val data = Json.decodeFromString<KDateData>(it)
            assertEquals(1999, data.data.year)
            assertEquals(12, data.data.month)
            assertEquals(31, data.data.day)
        }
    }

    @Test
    fun serializeKZonedDateTime() {
        val data = KZonedDateTimeData(KZonedDateTime(2026, 4, 19, 12, 1, 2, 3, KZoneOffset(9, 0)))
        val json = Json.encodeToString(data)
        assertEquals("{\"data\":\"2026-04-19T12:01:02.003+09:00\"}", json)

        val data2 = KZonedDateTimeData(KZonedDateTime(2026, 4, 19, 12, 1, 2, 3, KZoneOffset(-5, 30)))
        val json2 = Json.encodeToString(data2)
        assertEquals("{\"data\":\"2026-04-19T12:01:02.003-05:30\"}", json2)
    }

    @Test
    fun deserializeKZonedDateTime() {
        "{\"data\":\"2026-04-19T12:01:02.003+09:00\"}".let {
            val data = Json.decodeFromString<KZonedDateTimeData>(it)
            assertEquals(2026, data.data.year)
            assertEquals(4, data.data.month)
            assertEquals(19, data.data.day)
            assertEquals(12, data.data.hour)
            assertEquals(1, data.data.minute)
            assertEquals(2, data.data.second)
            assertEquals(3, data.data.millisecond)
            assertEquals(9, data.data.zoneOffset.hours)
            assertEquals(0, data.data.zoneOffset.minutes)
        }
        "{\"data\":\"2026-01-01T00:00:00.000Z\"}".let {
            val data = Json.decodeFromString<KZonedDateTimeData>(it)
            assertEquals(2026, data.data.year)
            assertEquals(1, data.data.month)
            assertEquals(1, data.data.day)
            assertEquals(0, data.data.hour)
            assertEquals(0, data.data.minute)
            assertEquals(0, data.data.second)
            assertEquals(0, data.data.millisecond)
            assertEquals(0, data.data.zoneOffset.hours)
            assertEquals(0, data.data.zoneOffset.minutes)
        }
        "{\"data\":\"2026-12-31T23:59:59.999-05:30\"}".let {
            val data = Json.decodeFromString<KZonedDateTimeData>(it)
            assertEquals(2026, data.data.year)
            assertEquals(12, data.data.month)
            assertEquals(31, data.data.day)
            assertEquals(23, data.data.hour)
            assertEquals(59, data.data.minute)
            assertEquals(59, data.data.second)
            assertEquals(999, data.data.millisecond)
            assertEquals(-5, data.data.zoneOffset.hours)
            assertEquals(30, data.data.zoneOffset.minutes)
        }
        "{\"data\":\"2026-06-15T09:30:45.123+05:45\"}".let {
            val decoded = Json.decodeFromString<KZonedDateTimeData>(it)
            val reEncoded = Json.encodeToString(decoded)
            val reDecoded = Json.decodeFromString<KZonedDateTimeData>(reEncoded)
            assertEquals(decoded.data.year, reDecoded.data.year)
            assertEquals(decoded.data.month, reDecoded.data.month)
            assertEquals(decoded.data.day, reDecoded.data.day)
            assertEquals(decoded.data.hour, reDecoded.data.hour)
            assertEquals(decoded.data.minute, reDecoded.data.minute)
            assertEquals(decoded.data.second, reDecoded.data.second)
            assertEquals(decoded.data.millisecond, reDecoded.data.millisecond)
            assertEquals(decoded.data.zoneOffset.hours, reDecoded.data.zoneOffset.hours)
            assertEquals(decoded.data.zoneOffset.minutes, reDecoded.data.zoneOffset.minutes)
        }
    }

    @Test
    fun serializeKDuration() {
        val data = KDurationData(KDuration.of(50, KFixedTimeUnit.Hour))
        val json = Json.encodeToString(data)
        assertEquals("{\"data\":{\"millis\":180000000}}", json)

        val data2 = KDurationData(KDuration.of(160, KFixedTimeUnit.MilliSecond))
        val json2 = Json.encodeToString(data2)
        assertEquals("{\"data\":{\"millis\":160}}", json2)
    }

    @Test
    fun deserializeKDuration() {
        "{\"data\":{\"millis\":180000000}}".let {
            val data = Json.decodeFromString<KDurationData>(it)
            assertEquals(180000000, data.data.millis)
        }
        "{\"data\":{\"millis\":160}}".let {
            val data = Json.decodeFromString<KDurationData>(it)
            assertEquals(160, data.data.millis)
        }
    }
}