package com.streamapi.custom.workers

import android.net.Uri
import com.streamapi.custom.StreamAPIException
import com.streamapi.custom.dto.Media
import com.streamapi.custom.tasks.StreamTask
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.io.IOException

internal object StreamTaskWorker {
    private const val JWPLAYER = "jwplayer"
    private const val STREAM_URL_SUFFIX = "/index-v1-a1.m3u8"
    private val STREAM_REGEX by lazy { "\\[\\{file:\"(.*)\"".toRegex() }
    private val ROUTE_REGEX by lazy { "download_video\\('(.*)'\\)".toRegex() }

    private const val MSG_SCRIPT_NOT_FOUND = "Required script that contains stream url not found."
    private const val MSG_STREAM_REGEX_NOT_MATCH = "Cannot find stream url using regex."
    private const val MSG_NOT_ENOUGH_SEGMENTS_FOR_STREAM =
        "Not enough segments to generate stream url."
    private const val MSG_NO_DOWNLOAD_ROUTES =
        "No available routes found."
    private const val MSG_NOT_ENOUGH_SEGMENTS_FOR_DOWNLOAD =
        "Not enough segments to generate download url."
    private const val MSG_ROUTE_REGEX_NOT_MATCH =
        "Cannot find hash codes to generate download route."
    private const val MSG_NO_FILE_SIZE =
        "No file size found in download url."


    private fun getHost(url: String): String {
        val uri = Uri.parse(url)
        return if (uri != null && uri.host != null) uri.host ?: "" else ""
    }

    fun get(url: String): StreamTask {
        val host = getHost(url)

        return try {
            val document = Jsoup.connect(url).get()
            val scriptElements = document.select("script")
            var selectedScript: String? = null

            for (scriptElement in scriptElements) {
                val script = scriptElement.toString()
                if (script.contains(JWPLAYER)) {
                    selectedScript = script
                    break
                }
            }

            if (selectedScript != null) {
                val result = STREAM_REGEX.find(selectedScript)
                if (result != null && result.groupValues.isNotEmpty()) {
                    val rawUrl = result.groupValues[1]
                    val segments = rawUrl.split(",")

                    if (segments.size >= 3) {
                        val streamUrlList = mutableListOf<String>()
                        for (i in 1..segments.size - 2) {
                            streamUrlList.add("${segments[0]}${segments[i]}$STREAM_URL_SUFFIX")
                        }

                        downloadRoutesTask(document, streamUrlList, host)
                    } else {
                        taskWithException(StreamAPIException(MSG_NOT_ENOUGH_SEGMENTS_FOR_STREAM))
                    }
                } else {
                    taskWithException(StreamAPIException(MSG_STREAM_REGEX_NOT_MATCH))
                }
            } else {
                taskWithException(StreamAPIException(MSG_SCRIPT_NOT_FOUND))
            }
        } catch (exception: IOException) {
            taskWithException(exception)
        }
    }

    private fun taskWithException(exception: Exception) =
        StreamTask(false, null, exception)

    private fun downloadRoutesTask(
        document: Document,
        streamUrlList: MutableList<String>,
        host: String
    ): StreamTask {
        val streams = arrayListOf<Media>()

        val trElements: Elements? = document.select("div#content table.tbl1 tbody tr:has(td)")
        if (trElements != null) {
            trElements.forEachIndexed { index, trElement ->
                val tdElements = trElement.select("td")
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
                                val media = Media(
                                    quality = aElement.text(),
                                    resolution = rawDetails[0].trim(),
                                    fileSize = rawDetails[1].trim(),
                                    url = streamUrlList[index],
                                    downloadRoute = downloadRoute
                                )
                                streams.add(media)
                            } else return taskWithException(StreamAPIException(MSG_NO_FILE_SIZE))
                        } else return taskWithException(StreamAPIException(MSG_ROUTE_REGEX_NOT_MATCH))
                    } else return taskWithException(
                        StreamAPIException(MSG_NOT_ENOUGH_SEGMENTS_FOR_DOWNLOAD)
                    )
                }
            }
            return StreamTask(true, streams, null)
        } else return taskWithException(StreamAPIException(MSG_NO_DOWNLOAD_ROUTES))
    }
}