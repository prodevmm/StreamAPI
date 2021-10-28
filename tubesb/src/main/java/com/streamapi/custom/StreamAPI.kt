package com.streamapi.custom

import androidx.core.util.PatternsCompat
import com.streamapi.custom.dto.Route
import com.streamapi.custom.tasks.DirectLinkTask
import com.streamapi.custom.tasks.RouteTask
import com.streamapi.custom.tasks.StreamTask
import com.streamapi.custom.workers.DirectLinkWorker
import com.streamapi.custom.workers.RouteWorker
import com.streamapi.custom.workers.StreamWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StreamAPI internal constructor(private val builder: StreamBuilder) {

    companion object {
        const val MSG_INVALID_URL = "Input URL is invalid."

        fun fetchDirectLink(
            route: Route,
            callback: DirectLinkCallback
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                val task = DirectLinkWorker.get(route.downloadRoute)
                CoroutineScope(Dispatchers.Main).launch { callback.onResponse(task) }
            }
        }

        fun fetchRoutes(url: String, callback: RouteCallback) {
            if (PatternsCompat.WEB_URL.matcher(url).matches()) {
                CoroutineScope(Dispatchers.IO).launch {
                    val task = RouteWorker(url).get()
                    CoroutineScope(Dispatchers.Main).launch { callback.onResponse(task) }
                }
            } else {
                callback.onResponse(RouteTask(false, null, StreamAPIException(MSG_INVALID_URL)))
            }
        }
    }

    interface StreamCallback {
        fun onResponse(streamTask: StreamTask)
    }

    interface RouteCallback {
        fun onResponse(routeTask: RouteTask)
    }

    interface DirectLinkCallback {
        fun onResponse(directLinkTask: DirectLinkTask)
    }

    fun fetchStreams(callback: StreamCallback) {
        if (PatternsCompat.WEB_URL.matcher(builder.url).matches()) {
            StreamWorker(
                builder.context,
                builder.url,
                builder.timeout,
                builder.skipResolution,
                builder.resolutionProcessGap,
                callback
            ).start()
        } else {
            callback.onResponse(StreamTask(false, null, StreamAPIException(MSG_INVALID_URL)))
        }
    }


}