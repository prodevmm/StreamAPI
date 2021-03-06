package com.streamapi.custom.tasks

import androidx.annotation.Nullable
import com.streamapi.custom.dto.Stream

data class StreamTask(
    val isSuccessful: Boolean,
    @Nullable private val _streams: ArrayList<Stream>?,
    @Nullable private val _exception: Exception?,
    val stacktrace: String = ""
) {
    val streams get() = _streams!!
    val exception: Exception get() = _exception!!
}