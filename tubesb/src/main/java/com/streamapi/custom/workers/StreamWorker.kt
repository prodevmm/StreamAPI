package com.streamapi.custom.workers

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import com.streamapi.custom.StreamAPI
import com.streamapi.custom.StreamAPIException
import com.streamapi.custom.dto.Stream
import com.streamapi.custom.tasks.StreamTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import java.util.*
import kotlin.concurrent.schedule


internal class StreamWorker(
    private val context: Context,
    private val url: String,
    private val timeout: Long,
    private val skipResolution: Boolean,
    private val resolutionProcessGap: Long,
    private val callback: StreamAPI.StreamCallback
) : BaseWorker() {
    companion object {
        private const val M3U8_EXTENSION = ".m3u8"
        private const val STREAM_URL_SUFFIX = "/index-v1-a1.m3u8"
        private const val TASK_TIMEOUT = "Task is timeout."
        private const val MSG_NOT_ENOUGH_SEGMENTS_FOR_STREAM =
            "Not enough segments to generate stream url."
    }

    private var webView: WebView? = null
    private var timerTask: TimerTask? = null
    private var foundM3U8 = false
    private val resolutions = arrayListOf<String>()

    @SuppressLint("SetJavaScriptEnabled")
    fun start() {
        try {
            appendStacktrace("- started StreamWorker\n-------")

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
                    if (!foundM3U8 && url?.contains(M3U8_EXTENSION, false) == true) {
                        foundM3U8 = true
                        appendStacktrace("- found m3u8 raw url\n\n$url")
                        if (skipResolution) {
                            destroyWebView()
                            appendStacktrace("- hidden WebView destroyed")
                            extractStreams(url)
                        } else {
                            evaluateResolutionExtractorJs(url)
                        }
                    }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    appendStacktrace("- finished hidden WebView page")
                    evaluateStreamExtractorJs()
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
                            stacktrace
                        )
                    )
                }
            }
            appendStacktrace("- started timer task")

        } catch (e: Exception) {
            taskWithException(e)
        }
    }

    private fun evaluateResolutionExtractorJs(url: String) {
        appendStacktrace("- injected resolution extractor js")
        Timer().schedule(resolutionProcessGap) {
            CoroutineScope(Dispatchers.Main).launch {
                webView?.evaluateJavascript(
                    "(function () {\n" +
                            "      let resolutionList = [];\n" +
                            "      let menuList = document.querySelectorAll(\n" +
                            "        \"div.jw-reset.jw-settings-submenu\"\n" +
                            "      );\n" +
                            "      if (menuList.length >= 2) {\n" +
                            "        menuList[1].childNodes.forEach((button) => {\n" +
                            "          var resolution = button.innerText;\n" +
                            "          if (!resolution.startsWith(\"Auto\")) {\n" +
                            "            resolutionList.push(resolution);\n" +
                            "          }\n" +
                            "        });\n" +
                            "        resolutionList.reverse();\n" +
                            "      }\n" +
                            "      return resolutionList;\n" +
                            "    })();"
                ) { resolutionJson ->
                    addResolutions(resolutionJson)
                    destroyWebView()
                    appendStacktrace("- hidden WebView destroyed")
                    extractStreams(url)
                }
            }
        }
    }

    private fun addResolutions(resolutionJson: String) {
        appendStacktrace("- extracted resolution json :\n$resolutionJson")
        try {
            val arr = JSONArray(resolutionJson.trim())
            if (arr.length() > 0) {
                for (i in 0 until arr.length()) {
                    resolutions.add(arr.getString(i))
                }
                appendStacktrace("- extracted resolutions : \n$resolutions")
            } else {
                appendStacktrace("- no resolutions found")
            }
        } catch (e: JSONException) {
            appendStacktrace(e.toString())
        }
    }

    private fun evaluateStreamExtractorJs() {
        webView?.evaluateJavascript(
            "let loadingDiv = document.getElementById(\"loading\");\n" +
                    "    let observer = new MutationObserver(function (mutationList) {\n" +
                    "      if (loadingDiv.style.display === \"none\") {\n" +
                    "        observer.disconnect();\n" +
                    "        let divList = document.querySelectorAll(\"div\");\n" +
                    "        if (divList.length >= 1) {\n" +
                    "          divList[divList.length - 1].click();\n" +
                    "        }\n" +
                    "      }\n" +
                    "    });\n" +
                    "    observer.observe(loadingDiv, { attributes: true, childList: false });",
            null
        )

        appendStacktrace("- injected stream extractor js")
    }

    private fun taskWithException(exception: Exception) {
        timerTask?.cancel()
        invokeCallback(StreamTask(false, null, exception, stacktrace))
    }

    private fun invokeCallback(streamTask: StreamTask) {
        CoroutineScope(Dispatchers.Main).launch {
            callback.onResponse(streamTask)
        }
    }

    private fun extractStreams(rawUrl: String) {
        timerTask?.cancel()
        appendStacktrace("- cancelled timer task")

        val segments = rawUrl.split(",")
        if (segments.size >= 3) {
            val streamUrlList = mutableListOf<String>()
            for (i in 1..segments.size - 2) {
                streamUrlList.add("${segments[0]}${segments[i]}$STREAM_URL_SUFFIX")
            }
            appendStacktrace("- extracted streams \n $streamUrlList")


            val streams = arrayListOf<Stream>()
            val noResolution = skipResolution || resolutions.size < streamUrlList.size
            streamUrlList.forEachIndexed { index, streamUrl ->
                val resolution = if (noResolution) "$index" else resolutions[index]
                streams.add(Stream(resolution, streamUrl))
            }

            invokeCallback(StreamTask(true, streams, null, stacktrace))
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
