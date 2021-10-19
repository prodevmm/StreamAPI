package com.example.cloudmbe;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cloudmbe.databinding.ActivityMainBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.streamapi.custom.StreamAPI;
import com.streamapi.custom.dto.Media;

import java.util.ArrayList;
import java.util.Objects;

public class JavaActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnFetch.setOnClickListener(v -> fetchStream());

    }

    private void fetchStream() {
        binding.btnFetch.setEnabled(false);
        binding.btnFetch.setText(R.string.btn_fetching_stream);

        String url = Objects.requireNonNull(binding.edt.getText()).toString();
        StreamAPI.INSTANCE.fetch(this, url, StreamAPI.DEFAULT_TIMEOUT, streamTask -> {
            if (streamTask.isSuccessful()) {
                showStreamsDialog(streamTask.getStreams());
            } else {
                showErrorDialog(streamTask.getException());
            }

            showStacktrace(streamTask.getStacktrace());

            binding.btnFetch.setEnabled(true);
            binding.btnFetch.setText(R.string.btn_fetch);
        });
    }

    private void showStacktrace(String stacktrace) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Stacktrace")
                .setMessage(stacktrace)
                .setNegativeButton(android.R.string.ok, null)
                .show();
    }

    private void showStreamsDialog(ArrayList<Media> streams) {
        ArrayAdapter<Media> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, streams);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Streams")
                .setNegativeButton(android.R.string.cancel, null)
                .setAdapter(adapter, (dialog, which) -> showStreamInfo(streams.get(which)))
                .show();
    }


    private void showStreamInfo(Media media) {
        String message = "Resolution : " + media.getResolution() +
                "\n\nFile size : " + media.getFileSize() +
                "\n\nStream URL : " + media.getUrl();

        new MaterialAlertDialogBuilder(this)
                .setTitle(media.getQuality())
                .setMessage(message)
                .setPositiveButton("Open", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(media.getUrl()));
                    startActivity(intent);
                })
                .setNegativeButton("Download", (dialog, which) -> fetchDownloadUrl(media))
                .show();
    }

    private void fetchDownloadUrl(Media media) {
        Snackbar.make(binding.getRoot(), "Fetching download link...", 1000).show();

        StreamAPI.INSTANCE.fetchDirectLink(media, directLinkTask -> {
            if (directLinkTask.isSuccessful()) {
                new MaterialAlertDialogBuilder(JavaActivity.this)
                        .setTitle("Success")
                        .setMessage(directLinkTask.getUrl())
                        .setPositiveButton("Open", (dialog, which) -> {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(media.getUrl()));
                            startActivity(intent);
                        })
                        .show();
            } else showErrorDialog(directLinkTask.getException());
        });
    }

    private void showErrorDialog(Exception exception) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(exception.getClass().getSimpleName())
                .setMessage(exception.getMessage())
                .setNegativeButton(android.R.string.ok, null)
                .show();
    }


}
