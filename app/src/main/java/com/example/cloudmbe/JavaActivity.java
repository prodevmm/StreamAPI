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
import com.streamapi.custom.StreamBuilder;
import com.streamapi.custom.dto.Route;
import com.streamapi.custom.dto.Stream;

import java.util.ArrayList;
import java.util.Objects;

public class JavaActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnFetchRoutes.setOnClickListener(v -> fetchRoutes());
        binding.btnFetchStreams.setOnClickListener(v -> fetchStreams());

    }

    private void fetchRoutes() {
        binding.btnFetchRoutes.setEnabled(false);
        binding.btnFetchRoutes.setText(R.string.btn_fetching);

        String url = Objects.requireNonNull(binding.edt.getText()).toString();
        // use StreamAPI.INSTANCE.fetchRoutes if not works
        StreamAPI.Companion.fetchRoutes(url, routeTask -> {
            if (routeTask.isSuccessful()) {
                showRouteList(routeTask.getRoutes());
            } else {
                showErrorDialog(routeTask.getException());
            }
            showStacktrace(routeTask.getStacktrace());

            binding.btnFetchRoutes.setEnabled(true);
            binding.btnFetchRoutes.setText(R.string.btn_fetch_routes);
        });

    }

    private void fetchStreams() {
        binding.btnFetchStreams.setEnabled(false);
        binding.btnFetchStreams.setText(R.string.btn_fetching);

        String url = Objects.requireNonNull(binding.edt.getText()).toString();
        long timeout = Long.parseLong(Objects.requireNonNull(binding.edtTimeout.getText()).toString());
        long gap = Long.parseLong(Objects.requireNonNull(binding.edtResolutionProcessGap.getText()).toString());
        boolean skipResolutionProcess = binding.skipResolutionProcess.isChecked();

        StreamBuilder builder = new StreamBuilder(this, url)
                .setTimeout(timeout) // optional
                .setResolutionProcessGap(gap); // optional

        if (skipResolutionProcess) {
            builder.skipResolutionProcess(); // optional
        }

        builder.build().fetchStreams(streamTask -> {
            if (streamTask.isSuccessful()) {
                showStreamList(streamTask.getStreams());
            } else {
                showErrorDialog(streamTask.getException());
            }
            showStacktrace(streamTask.getStacktrace());

            binding.btnFetchStreams.setEnabled(true);
            binding.btnFetchStreams.setText(R.string.btn_fetch_streams);
        });

    }

    private void showStacktrace(String stacktrace) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Stacktrace")
                .setMessage(stacktrace)
                .setNegativeButton(android.R.string.ok, null)
                .show();
    }

    private void showRouteList(ArrayList<Route> routes) {
        ArrayAdapter<Route> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, routes);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Routes")
                .setNegativeButton(android.R.string.cancel, null)
                .setAdapter(adapter, (dialog, which) -> showRouteInfo(routes.get(which)));
    }

    private void showRouteInfo(Route route) {
        String message = "Resolution : " + route.getResolution() + "\n\nFile size : " + route.getFileSize();
        new MaterialAlertDialogBuilder(this)
                .setTitle(route.getQuality())
                .setMessage(message)
                .setNegativeButton("Download", (dialog, which) -> fetchDownloadUrl(route))
                .show();
    }

    private void fetchDownloadUrl(Route route) {
        Snackbar.make(binding.getRoot(), "Fetching download link...", 1000).show();

        StreamAPI.Companion.fetchDirectLink(route, directLinkTask -> {
            if (directLinkTask.isSuccessful()) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Success")
                        .setMessage(directLinkTask.getUrl())
                        .setPositiveButton("Open", (dialog, which) -> {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(directLinkTask.getUrl()));
                            startActivity(intent);
                        })
                        .show();
            } else showErrorDialog(directLinkTask.getException());
        });
    }

    private void showStreamList(ArrayList<Stream> streams) {
        ArrayAdapter<Stream> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, streams);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Streams")
                .setNegativeButton(android.R.string.cancel, null)
                .setAdapter(adapter, (dialog, which) -> showStreamInfo(streams.get(which)))
                .show();
    }

    private void showStreamInfo(Stream stream) {
        String message = "Resolution : " + stream.getResolution() + "\n\nURL : " + stream.getUrl();
        new MaterialAlertDialogBuilder(this)
                .setTitle(stream.getResolution())
                .setMessage(message)
                .setNegativeButton("Play", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(stream.getUrl()));
                    startActivity(intent);
                })
                .show();
    }

    private void showErrorDialog(Exception exception) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(exception.getClass().getSimpleName())
                .setMessage(exception.getMessage())
                .setNegativeButton(android.R.string.ok, null)
                .show();
    }

}
