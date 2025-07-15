package com.mycompany.plugins.example;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.*;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
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

  @RequiresPermission(Manifest.permission.RECORD_AUDIO)
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

  @RequiresPermission(Manifest.permission.RECORD_AUDIO)
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
            try {
              overlayImageToVideo(videoFile, latestPhotoFile);
            } catch (IOException e) {
              e.printStackTrace();
            }
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

  private void overlayImageToVideo(File videoFile, File imageFile) throws IOException {
    Log.d("CustomCamera", "Overlaying image to video using MediaCodec...");

    // Step 1: Decode video with MediaExtractor
    MediaExtractor extractor = new MediaExtractor();
    extractor.setDataSource(videoFile.getAbsolutePath());

    int trackIndex = -1;
    for (int i = 0; i < extractor.getTrackCount(); i++) {
      MediaFormat format = extractor.getTrackFormat(i);
      String mime = format.getString(MediaFormat.KEY_MIME);
      if (mime.startsWith("video/")) {
        trackIndex = i;
        break;
      }
    }
    if (trackIndex < 0) throw new RuntimeException("No video track found");

    extractor.selectTrack(trackIndex);
    MediaFormat inputFormat = extractor.getTrackFormat(trackIndex);

    // Step 2: Set up decoder
    MediaCodec decoder = MediaCodec.createDecoderByType(inputFormat.getString(MediaFormat.KEY_MIME));
    Surface surface = MediaCodec.createPersistentInputSurface();
    decoder.configure(inputFormat, surface, null, 0);
    decoder.start();

    // Step 3: Set up encoder and muxer (mocked — implementation required)
    // You should set up MediaCodec encoder and use MediaMuxer to write final output
    // Apply your image overlay logic using OpenGL or Canvas on the Surface used by encoder

    // Decode loop (simplified and partial)
    ByteBuffer[] inputBuffers = decoder.getInputBuffers();
    MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

    boolean isEOS = false;
    long timeoutUs = 10000;

    while (!isEOS) {
      int inIndex = decoder.dequeueInputBuffer(timeoutUs);
      if (inIndex >= 0) {
        ByteBuffer buffer = inputBuffers[inIndex];
        int sampleSize = extractor.readSampleData(buffer, 0);
        if (sampleSize < 0) {
          decoder.queueInputBuffer(inIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
          isEOS = true;
        } else {
          decoder.queueInputBuffer(inIndex, 0, sampleSize, extractor.getSampleTime(), 0);
          extractor.advance();
        }
      }

      int outIndex = decoder.dequeueOutputBuffer(info, timeoutUs);
      if (outIndex >= 0) {
        // Render decoded frame to surface (implement overlay here)
        decoder.releaseOutputBuffer(outIndex, true);
      }
    }

    decoder.stop();
    decoder.release();
    extractor.release();
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
