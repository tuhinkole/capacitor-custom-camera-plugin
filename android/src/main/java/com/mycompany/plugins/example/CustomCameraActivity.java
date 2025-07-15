package com.mycompany.plugins.example;

import android.Manifest;
import android.content.pm.PackageManager;
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
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.ReturnCode;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class CustomCameraActivity extends AppCompatActivity {

    private PreviewView previewView;
    private Button recordButton, captureButton;
    private ImageView flashOverlay;

    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private ImageCapture imageCapture;
    private VideoCapture<Recorder> videoCapture;
    private Recording currentRecording;

    private File videoFile;
    private File latestPhotoFile;
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_camera);

        previewView = findViewById(R.id.previewView);
        recordButton = findViewById(R.id.recordButton);
        captureButton = findViewById(R.id.captureButton);
        flashOverlay = findViewById(R.id.flashOverlay);

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        recordButton.setOnClickListener(v -> {
            if (currentRecording == null) {
                startRecording();
            } else {
                stopRecording();
            }
        });

        captureButton.setOnClickListener(v -> {
            takePhotoWithFlash();
        });
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
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

        imageCapture = new ImageCapture.Builder().build();

        Recorder recorder = new Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build();

        videoCapture = VideoCapture.withOutput(recorder);

        cameraProvider.unbindAll();
        camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, videoCapture);
    }

    private void startRecording() {
        recordButton.setText("Stop");

        videoFile = new File(getExternalFilesDir(null),
                "recorded_" + System.currentTimeMillis() + ".mp4");

        FileOutputOptions outputOptions = new FileOutputOptions.Builder(videoFile).build();

        currentRecording = videoCapture.getOutput()
                .prepareRecording(this, outputOptions)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(this), videoRecordEvent -> {
                    if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
                        Log.d("CustomCamera", "Video saved: " + videoFile.getAbsolutePath());
                        currentRecording = null;

                        if (latestPhotoFile != null) {
                            overlayPhotoOnVideo(latestPhotoFile.getAbsolutePath(), videoFile.getAbsolutePath());
                        }
                    }
                });
    }

    private void stopRecording() {
        recordButton.setText("Record");
        if (currentRecording != null) {
            currentRecording.stop();
            currentRecording = null;
        }
    }

    private void takePhotoWithFlash() {
        showFlashEffect();

        File imageFile = new File(getExternalFilesDir(null),
                "photo_" + System.currentTimeMillis() + ".jpg");

        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(imageFile).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Log.d("CustomCamera", "Image saved: " + imageFile.getAbsolutePath());
                        latestPhotoFile = imageFile;
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

    private void overlayPhotoOnVideo(String imagePath, String videoPath) {
        String outputPath = new File(getExternalFilesDir(null),
                "final_" + System.currentTimeMillis() + ".mp4").getAbsolutePath();

        String ffmpegCommand = String.format(
                "-i %s -i %s -filter_complex [0:v][1:v] overlay=W-w-20:20 -c:a copy %s",
                videoPath,
                imagePath,
                outputPath
        );

        FFmpegKit.executeAsync(ffmpegCommand, session -> {
            if (ReturnCode.isSuccess(session.getReturnCode())) {
                Log.d("FFmpeg", "Overlay completed. File saved at: " + outputPath);
            } else {
                Log.e("FFmpeg", "Overlay failed: " + session.getFailStackTrace());
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS && allPermissionsGranted()) {
            startCamera();
        }
    }
} 