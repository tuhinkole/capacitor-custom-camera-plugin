package com.mycompany.plugins.example;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.*;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.ExecutionException;

public class CustomCameraActivity extends AppCompatActivity {

    private PreviewView previewView;
    private Button recordButton, captureButton;
    private ImageView flashOverlay;

    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private ImageCapture imageCapture;
    private VideoCapture<Recorder> videoCapture;

    private boolean isRecording = false;
    private Recording currentRecording;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_camera);

        previewView = findViewById(R.id.previewView);
        recordButton = findViewById(R.id.recordButton);
        captureButton = findViewById(R.id.captureButton);
        flashOverlay = findViewById(R.id.flashOverlay);

        startCamera();

        recordButton.setOnClickListener(v -> {
            if (!isRecording) {
                startRecording();
            } else {
                stopRecording();
            }
        });

        captureButton.setOnClickListener(v -> {
            takePhotoWithFlash();
        });
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        Recorder recorder = new Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build();

        videoCapture = VideoCapture.withOutput(recorder);

        cameraProvider.unbindAll();
        camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, videoCapture);
    }

    private void startRecording() {
        isRecording = true;
        recordButton.setText("Stop");

        File videoFile = new File(getExternalFilesDir(null), "recorded_" + System.currentTimeMillis() + ".mp4");
        FileOutputOptions outputOptions = FileOutputOptions.builder(videoFile).build();

        currentRecording = videoCapture.getOutput()
                .prepareRecording(this, outputOptions)
                .start(ContextCompat.getMainExecutor(this), videoRecordEvent -> {
                    if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
                        Log.d("CustomCamera", "Video saved: " +
                                ((VideoRecordEvent.Finalize) videoRecordEvent).getOutputResults().getOutputUri());
                    }
                });
    }

    private void stopRecording() {
        if (currentRecording != null) {
            currentRecording.stop();
            currentRecording = null;
        }
        isRecording = false;
        recordButton.setText("Record");
    }

    private void takePhotoWithFlash() {
        showFlashEffect();

        File imageFile = new File(getExternalFilesDir(null), "photo_" + System.currentTimeMillis() + ".jpg");

        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(imageFile).build();

        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Log.d("CustomCamera", "Image saved: " + imageFile.getAbsolutePath());
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e("CustomCamera", "Photo capture failed", exception);
                    }
                });
    }

    private void showFlashEffect() {
        flashOverlay.setAlpha(1f);
        flashOverlay.setVisibility(View.VISIBLE);

        flashOverlay.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> flashOverlay.setVisibility(View.GONE))
                .start();
    }
}
