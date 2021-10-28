package com.example.cloudmbe

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.example.cloudmbe.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.streamapi.custom.StreamAPI
import com.streamapi.custom.StreamBuilder
import com.streamapi.custom.dto.Route
import com.streamapi.custom.dto.Stream
import com.streamapi.custom.tasks.DirectLinkTask
import com.streamapi.custom.tasks.RouteTask
import com.streamapi.custom.tasks.StreamTask

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnFetchRoutes.setOnClickListener { fetchRoutes() }
        binding.btnFetchStreams.setOnClickListener { fetchStreams() }


    }

    private fun fetchRoutes() {
        with(binding.btnFetchRoutes) {
            isEnabled = false
            setText(R.string.btn_fetching)
        }

        val url = binding.edt.text.toString()
        StreamAPI.fetchRoutes(url, object : StreamAPI.RouteCallback {
            override fun onResponse(routeTask: RouteTask) {
                if (routeTask.isSuccessful) {
                    showRouteList(routeTask.routes)
                } else {
                    showErrorDialog(routeTask.exception)
                }
                showStacktrace(routeTask.stacktrace)

                with(binding.btnFetchRoutes) {
                    isEnabled = true
                    setText(R.string.btn_fetch_routes)
                }
            }
        })

    }

    private fun fetchStreams() {
        with(binding.btnFetchStreams) {
            isEnabled = false
            setText(R.string.btn_fetching)
        }
        val url = binding.edt.text.toString()
        val timeout = binding.edtTimeout.text.toString().toLong()
        val gap = binding.edtResolutionProcessGap.text.toString().toLong()
        val skipResolutionProcess = binding.skipResolutionProcess.isChecked

        val builder = StreamBuilder(this, url)
            .setTimeout(timeout) // optional
            .setResolutionProcessGap(gap) // optional

        if (skipResolutionProcess) {
            builder.skipResolutionProcess() // optional
        }

        builder.build().fetchStreams(object : StreamAPI.StreamCallback {
            override fun onResponse(streamTask: StreamTask) {
                if (streamTask.isSuccessful) {
                    showStreamList(streamTask.streams)
                } else {
                    showErrorDialog(streamTask.exception)
                }
                showStacktrace(streamTask.stacktrace)

                with(binding.btnFetchStreams) {
                    isEnabled = true
                    setText(R.string.btn_fetch_streams)
                }
            }

        })

    }

    private fun showStacktrace(stacktrace: String) {
        MaterialAlertDialogBuilder(this@MainActivity)
            .setTitle("Stacktrace")
            .setMessage(stacktrace)
            .setNegativeButton(android.R.string.ok, null)
            .show()
    }

    private fun showRouteList(routes: ArrayList<Route>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, routes)

        MaterialAlertDialogBuilder(this)
            .setTitle("Routes")
            .setNegativeButton(android.R.string.cancel, null)
            .setAdapter(adapter) { _, position: Int ->
                showRouteInfo(routes[position])
            }
            .show()
    }

    private fun showRouteInfo(route: Route) {
        val message = "Resolution : ${route.resolution}\n\nFile size : ${route.fileSize}"
        MaterialAlertDialogBuilder(this)
            .setTitle(route.quality)
            .setMessage(message)
            .setNegativeButton("Download") { _, _ -> fetchDownloadUrl(route) }
            .show()
    }

    private fun fetchDownloadUrl(route: Route) {
        Snackbar.make(binding.root, "Fetching download link...", 1000).show()

        StreamAPI.fetchDirectLink(route, object : StreamAPI.DirectLinkCallback {
            override fun onResponse(directLinkTask: DirectLinkTask) {
                if (directLinkTask.isSuccessful) {
                    MaterialAlertDialogBuilder(this@MainActivity)
                        .setTitle("Success")
                        .setMessage(directLinkTask.url)
                        .setPositiveButton("Open") { _, _ ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(directLinkTask.url))
                            startActivity(intent)
                        }
                        .show()
                } else showErrorDialog(directLinkTask.exception)
            }
        })
    }

    private fun showStreamList(streams: ArrayList<Stream>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, streams)

        MaterialAlertDialogBuilder(this)
            .setTitle("Streams")
            .setNegativeButton(android.R.string.cancel, null)
            .setAdapter(adapter) { _, position: Int ->
                showStreamInfo(streams[position])
            }
            .show()
    }

    private fun showStreamInfo(stream: Stream) {
        val message = "Resolution : ${stream.resolution}\n\nURL : ${stream.url}"
        MaterialAlertDialogBuilder(this)
            .setTitle(stream.resolution)
            .setMessage(message)
            .setNegativeButton("Play") { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(stream.url))
                startActivity(intent)
            }
            .show()
    }

    private fun showErrorDialog(exception: Exception) {
        MaterialAlertDialogBuilder(this)
            .setTitle(exception.javaClass.simpleName)
            .setMessage(exception.message)
            .setNegativeButton(android.R.string.ok, null)
            .show()
    }
}