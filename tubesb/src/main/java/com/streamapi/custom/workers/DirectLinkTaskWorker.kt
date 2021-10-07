package com.streamapi.custom.workers

import com.streamapi.custom.StreamAPIException
import com.streamapi.custom.tasks.DirectLinkTask
import org.jsoup.Jsoup
import java.io.IOException


internal object DirectLinkTaskWorker {
    private const val MSG_DIRECT_LINK_NOT_FOUND = "Direct link not found in download route."

    fun get(url: String): DirectLinkTask {
        return try {
            val document = Jsoup.connect(url).get()
            val aElement = document.selectFirst("div#container div.contentbox span a")
            return if (aElement != null) {
                val directUrl = aElement.attr("href")
                DirectLinkTask(true, directUrl, null)
            } else {
                DirectLinkTask(false, null, StreamAPIException(MSG_DIRECT_LINK_NOT_FOUND))
            }
        } catch (exception: IOException) {
            DirectLinkTask(false, null, exception)
        }
    }
}