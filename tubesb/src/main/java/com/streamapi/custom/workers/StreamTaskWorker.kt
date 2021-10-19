package com.streamapi.custom.workers

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import com.streamapi.custom.StreamAPI
import com.streamapi.custom.StreamAPIException
import com.streamapi.custom.tasks.StreamTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.schedule


internal class StreamTaskWorker(
    private val context: Context,
    private val url: String,
    private val timeout: Long,
    private val callback: StreamAPI.Callback
) {
    companion object {
        private const val M3U8_EXTENSION = ".m3u8"
        private const val STREAM_URL_SUFFIX = "/index-v1-a1.m3u8"
        private const val TASK_TIMEOUT = "Task is timeout."
        private const val MSG_NOT_ENOUGH_SEGMENTS_FOR_STREAM =
            "Not enough segments to generate stream url."
    }

    private val stackTraceBuilder = StringBuilder()
    private var webView: WebView? = null
    private val dateFormat = SimpleDateFormat("h:mm:ss a", Locale.ENGLISH)

    private var timerTask: TimerTask? = null

    private fun appendStacktrace(stack: String) {
        stackTraceBuilder
            .append(dateFormat.format(Date(System.currentTimeMillis())))
            .append("\n")
            .append(stack)
            .append("\n\n")
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun start() {
        try {
            appendStacktrace("- started STREAM TASK\n===========")

            val uri = Uri.parse(url)
            appendStacktrace("- original url : $url")

            val playUrl = "${uri.scheme}://${uri.host}/play${uri.path}"
            appendStacktrace("- formatted url into\n$playUrl")

            webView = WebView(context)
            val settings = webView?.settings
            settings?.allowContentAccess = true
            settings?.allowFileAccess = true
            settings?.javaScriptEnabled = true
            settings?.loadWithOverviewMode = true
            settings?.useWideViewPort = true
            settings?.databaseEnabled = true
            settings?.domStorageEnabled = true

            webView?.webChromeClient = WebChromeClient()
            webView?.webViewClient = object : WebViewClient() {
                override fun onLoadResource(view: WebView?, url: String?) {
                    super.onLoadResource(view, url)
                    if (url?.contains(M3U8_EXTENSION, false) == true) {
                        appendStacktrace("- found m3u8 raw url")
                        destroyWebView()
                        appendStacktrace("- hidden WebView destroyed")
                        extractStreams(url)
                    }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    appendStacktrace("- finished hidden WebView page")

                    webView?.evaluateJavascript(
                        "let loadingDiv = document.getElementById(\"loading\")\n" +
                                "    let observer = new MutationObserver(function(mutationList){\n" +
                                "        if (loadingDiv.style.display === \"none\"){\n" +
                                "            observer.disconnect();\n" +
                                "            var lastDiv = document.querySelectorAll(\"div\");\n" +
                                "            if (lastDiv.length > 0){\n" +
                                "                lastDiv[lastDiv.length -1].click();\n" +
                                "            }\n" +
                                "        }\n" +
                                "    });\n" +
                                "    observer.observe(loadingDiv, { attributes: true, childList: false });",
                        null
                    )

                    appendStacktrace("- injected javascript codes")
                }
            }

            webView?.loadUrl(playUrl)
            appendStacktrace("- created hidden WebView")

            timerTask = Timer().schedule(timeout) {
                CoroutineScope(Dispatchers.Main).launch {
                    destroyWebView()
                    callback.onResponse(
                        StreamTask(
                            false,
                            null,
                            StreamAPIException("$TASK_TIMEOUT\ntimeout : $timeout ms"),
                            stackTraceBuilder.toString()
                        )
                    )
                }
            }
            appendStacktrace("- started timer task")

        } catch (e: Exception) {
            taskWithException(e)
        }
    }

    private fun taskWithException(exception: Exception) {
        timerTask?.cancel()
        invokeCallback(StreamTask(false, null, exception, stackTraceBuilder.toString()))
    }

    private fun invokeCallback(streamTask: StreamTask) {
        CoroutineScope(Dispatchers.Main).launch {
            callback.onResponse(streamTask)
        }
    }

    private fun extractStreams(rawUrl: String) {
        val segments = rawUrl.split(",")
        if (segments.size >= 3) {
            val streamUrlList = mutableListOf<String>()
            for (i in 1..segments.size - 2) {
                streamUrlList.add("${segments[0]}${segments[i]}$STREAM_URL_SUFFIX")
            }
            appendStacktrace("- extracted streams \n $streamUrlList")

            CoroutineScope(Dispatchers.IO).launch {
                appendStacktrace("- initialized IO CoroutineScope for download link extractor task")
                val streamTask =
                    DownloadExtractWorker(streamUrlList, url, stackTraceBuilder.toString()).get()
                appendStacktrace("- finished download link extractor task")

                timerTask?.cancel()
                appendStacktrace("- cancelled timer task")
                invokeCallback(streamTask)
            }
        } else {
            taskWithException(StreamAPIException(MSG_NOT_ENOUGH_SEGMENTS_FOR_STREAM))
        }
    }

    private fun destroyWebView() {
        if (webView != null) {
            webView?.loadUrl("about:blank")
            webView?.destroy()
            webView = null
        }
    }
}
