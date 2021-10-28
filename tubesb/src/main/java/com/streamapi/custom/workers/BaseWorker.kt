package com.streamapi.custom.workers

import java.text.SimpleDateFormat
import java.util.*

internal open class BaseWorker {
    private val dateFormat = SimpleDateFormat("h:mm:ss a", Locale.ENGLISH)
    private val stackTraceBuilder = StringBuilder()

    fun appendStacktrace(stack: String, directly: Boolean = false) {
        if (directly) {
            stackTraceBuilder.append(stack)
        } else {
            stackTraceBuilder
                .append(dateFormat.format(Date(System.currentTimeMillis())))
                .append("\n")
                .append(stack)
                .append("\n\n")
        }
    }

    val stacktrace get() = stackTraceBuilder.toString()
}