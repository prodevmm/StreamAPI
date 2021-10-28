package com.streamapi.custom.workers

import android.net.Uri
import com.streamapi.custom.StreamAPIException
import com.streamapi.custom.dto.Route
import com.streamapi.custom.tasks.RouteTask
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import java.io.IOException

internal class RouteWorker(
    private val url: String
) : BaseWorker() {

    companion object {
        private val ROUTE_REGEX by lazy { "download_video\\('(.*)'\\)".toRegex() }
        private const val MSG_NO_DOWNLOAD_ROUTES =
            "No available routes found."
        private const val MSG_NOT_ENOUGH_SEGMENTS_FOR_DOWNLOAD =
            "Not enough segments to generate download url."
        private const val MSG_ROUTE_REGEX_NOT_MATCH =
            "Cannot find hash codes to generate download route."
        private const val MSG_NO_FILE_SIZE =
            "No file size found in download url."
    }

    private fun getHost(url: String): String {
        val uri = Uri.parse(url)
        return if (uri != null && uri.host != null) uri.host ?: "" else ""
    }

    fun get(): RouteTask {
        val host = getHost(url)

        return try {
            appendStacktrace("- started RouteWorker\n\t--------")
            appendStacktrace("- target url $url")
            val document = Jsoup.connect(url).get()
            appendStacktrace("- url has scraped")

            val streams = arrayListOf<Route>()

            val trElements: Elements? =
                document.select("div#content table.tbl1 tbody tr:has(td)")
            appendStacktrace("- selected tr elements : ${trElements?.size}")

            if (trElements != null) {
                trElements.forEachIndexed { index, trElement ->
                    val tdElements = trElement.select("td")
                    appendStacktrace("- selected td elements for $index tr : ${tdElements.size}")

                    if (tdElements.size >= 2) {
                        val aElement = tdElements[0].selectFirst("a")

                        if (aElement != null) {
                            val onClickScript = aElement.attr("onclick")
                            val result = ROUTE_REGEX.find(onClickScript)
                            if (result != null && result.groupValues.isNotEmpty()) {
                                val rawValues = result.groupValues[1].split("','")
                                val downloadRoute =
                                    "https://$host/dl?op=download_orig&id=${rawValues[0]}&mode=${rawValues[1]}&hash=${rawValues[2]}"
                                val rawDetails = tdElements[1].text().split(",")
                                if (rawDetails.size >= 2) {
                                    val media = Route(
                                        quality = aElement.text(),
                                        resolution = rawDetails[0].trim(),
                                        fileSize = rawDetails[1].trim(),
                                        downloadRoute = downloadRoute
                                    )
                                    streams.add(media)
                                } else return taskWithException(
                                    StreamAPIException(
                                        MSG_NO_FILE_SIZE
                                    )
                                )
                            } else return taskWithException(
                                StreamAPIException(
                                    MSG_ROUTE_REGEX_NOT_MATCH
                                )
                            )
                        } else return taskWithException(
                            StreamAPIException(MSG_NOT_ENOUGH_SEGMENTS_FOR_DOWNLOAD)
                        )
                    }
                }

                return RouteTask(true, streams, null, stacktrace)
            } else return taskWithException(StreamAPIException(MSG_NO_DOWNLOAD_ROUTES))
        } catch (exception: IOException) {
            taskWithException(exception)
        }
    }

    private fun taskWithException(exception: Exception) =
        RouteTask(false, null, exception, stacktrace)
}