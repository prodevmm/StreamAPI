package com.streamapi.custom.tasks

import androidx.annotation.Nullable
import com.streamapi.custom.dto.Route

data class RouteTask(
    val isSuccessful: Boolean,
    @Nullable private val _routes: ArrayList<Route>?,
    @Nullable private val _exception: Exception?,
    val stacktrace: String = ""
) {
    val routes get() = _routes!!
    val exception: Exception get() = _exception!!
}