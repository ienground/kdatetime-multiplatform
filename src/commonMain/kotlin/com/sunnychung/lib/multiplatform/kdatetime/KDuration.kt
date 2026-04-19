package com.sunnychung.lib.multiplatform.kdatetime

import com.sunnychung.lib.multiplatform.kdatetime.annotation.AndroidParcelize
import kotlinx.serialization.Serializable

@Serializable
@AndroidParcelize
open class KDuration internal constructor(val millis: Long) : KDateTimeFormattable,
    Comparable<KDuration>, AndroidParcelable {

    override fun toMilliseconds(): Long = millis

    fun toTimeUnitValue(targetUnit: KFixedTimeUnit): Long {
        return millis / targetUnit.ratioToMillis
    }

    fun toSeconds() = toTimeUnitValue(KFixedTimeUnit.Second)
    fun toMinutes() = toTimeUnitValue(KFixedTimeUnit.Minute)
    fun toHours() = toTimeUnitValue(KFixedTimeUnit.Hour)
    fun toDays() = toTimeUnitValue(KFixedTimeUnit.Day)
    fun toWeeks() = toTimeUnitValue(KFixedTimeUnit.Week)

    operator fun plus(other: KDuration): KDuration {
        return KDuration(millis + other.millis)
    }

    operator fun minus(other: KDuration): KDuration {
        return KDuration(millis - other.millis)
    }

    override fun compareTo(other: KDuration): Int {
        return toMilliseconds().compareTo(other.toMilliseconds())
    }

    override fun equals(other: Any?): Boolean {
        if (other is KDuration) {
            return toMilliseconds() == other.toMilliseconds()
        }
        return false
    }

    override fun toString(): String {
        return "${millis}ms"
    }

    companion object {
        fun of(value: Int, unit: KFixedTimeUnit) = KDuration(value * unit.ratioToMillis)
        fun of(value: Long, unit: KFixedTimeUnit) = KDuration(value * unit.ratioToMillis)

        val ZERO = KDuration(0)
    }
}
