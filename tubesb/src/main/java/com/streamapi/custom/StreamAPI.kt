package com.streamapi.custom

import android.content.Context
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
    @Suppress("unused")
    const val DEFAULT_TIMEOUT = 10000L

    interface Callback {
        fun onResponse(streamTask: StreamTask)
    }

    interface DirectLinkCallback {
        fun onResponse(directLinkTask: DirectLinkTask)
    }

    fun fetch(context: Context, url: String, timeout: Long, callback: Callback) {
        if (PatternsCompat.WEB_URL.matcher(url).matches()) {
            StreamTaskWorker(context, url, timeout, callback).start()
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