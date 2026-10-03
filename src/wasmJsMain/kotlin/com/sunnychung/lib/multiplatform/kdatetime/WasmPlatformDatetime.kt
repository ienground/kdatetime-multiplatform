@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.sunnychung.lib.multiplatform.kdatetime

import kotlin.js.js
import kotlin.math.roundToLong

internal actual fun kInstantOfCurrentTime(): KInstant {
    return KInstant(currentTimeMillis().roundToLong())
}

internal actual fun localZoneOffset(): KZoneOffset {
    val offsetMinutes = -timezoneOffsetMinutes()
    return KZoneOffset.fromMilliseconds(KDuration.of(offsetMinutes, KFixedTimeUnit.Minute).toMilliseconds())
}

private fun currentTimeMillis(): Double = js("Date.now()")

private fun timezoneOffsetMinutes(): Int = js("new Date().getTimezoneOffset()")
