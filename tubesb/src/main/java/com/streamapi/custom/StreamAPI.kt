package com.streamapi.custom

import androidx.core.util.PatternsCompat
import com.streamapi.custom.dto.Media
import com.streamapi.custom.tasks.DirectLinkTask
import com.streamapi.custom.tasks.StreamTask
import com.streamapi.custom.workers.DirectLinkTaskWorker
import com.streamapi.custom.workers.StreamTaskWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object StreamAPI {
    private const val MSG_INVALID_URL = "URL is invalid."

    abstract class Callback {
        abstract fun onResponse(streamTask: StreamTask)
    }

    abstract class DirectLinkCallback {
        abstract fun onResponse(directLinkTask: DirectLinkTask)
    }

    fun fetch(url: String, callback: Callback) {
        if (PatternsCompat.WEB_URL.matcher(url).matches()) {
            CoroutineScope(Dispatchers.IO).launch {
                val task = StreamTaskWorker.get(url)
                CoroutineScope(Dispatchers.Main).launch { callback.onResponse(task) }
            }
        } else {
            callback.onResponse(StreamTask(false, null, StreamAPIException(MSG_INVALID_URL)))
        }
    }

    fun fetchDirectLink(
        media: Media,
        callback: DirectLinkCallback
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val task = DirectLinkTaskWorker.get(media.downloadRoute)
            CoroutineScope(Dispatchers.Main).launch { callback.onResponse(task) }
        }
    }
}