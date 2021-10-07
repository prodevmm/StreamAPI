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
import com.streamapi.custom.dto.Media
import com.streamapi.custom.tasks.DirectLinkTask
import com.streamapi.custom.tasks.StreamTask

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnFetch.setOnClickListener { fetchStream() }
    }

    private fun fetchStream() {
        with(binding.btnFetch) {
            isEnabled = false
            setText(R.string.btn_fetching_stream)
        }

        val url = binding.edt.text.toString()
        StreamAPI.fetch(url, object : StreamAPI.Callback() {
            override fun onResponse(streamTask: StreamTask) {
                if (streamTask.isSuccessful) {
                    showStreamsDialog(streamTask.streams)
                } else showErrorDialog(streamTask.exception)

                with(binding.btnFetch) {
                    isEnabled = true
                    setText(R.string.btn_fetch)
                }
            }
        })


    }

    private fun showStreamsDialog(streams: ArrayList<Media>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, streams)

        MaterialAlertDialogBuilder(this)
            .setTitle("Streams")
            .setNegativeButton(android.R.string.cancel, null)
            .setAdapter(adapter) { _, position: Int ->
                showStreamInfo(streams[position])
            }
            .show()
    }

    private fun showStreamInfo(media: Media) {
        val message =
            "Resolution : ${media.resolution}\n\nFile size : ${media.fileSize}\n\nStream URL : ${media.url}"

        MaterialAlertDialogBuilder(this)
            .setTitle(media.quality)
            .setMessage(message)
            .setPositiveButton("Open") { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(media.url))
                startActivity(intent)
            }
            .setNegativeButton("Download") { _, _ -> fetchDownloadUrl(media) }
            .show()
    }

    private fun fetchDownloadUrl(media: Media) {
        Snackbar.make(binding.root, "Fetching download link...", 1000).show()

        StreamAPI.fetchDirectLink(media, object : StreamAPI.DirectLinkCallback() {
            override fun onResponse(directLinkTask: DirectLinkTask) {
                if (directLinkTask.isSuccessful) {
                    MaterialAlertDialogBuilder(this@MainActivity)
                        .setTitle("Success")
                        .setMessage(directLinkTask.url)
                        .setPositiveButton("Open") { _, _ ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(media.url))
                            startActivity(intent)
                        }
                        .show()
                } else showErrorDialog(directLinkTask.exception)
            }
        })
    }

    private fun showErrorDialog(exception: Exception) {
        MaterialAlertDialogBuilder(this)
            .setTitle(exception.javaClass.simpleName)
            .setMessage(exception.message)
            .setNegativeButton(android.R.string.ok, null)
            .show()
    }
}