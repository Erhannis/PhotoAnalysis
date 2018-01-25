package com.erhannis.photoanalysis;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.widget.ImageView;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Timer;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    @BindView(R.id.imageView) public ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        try {
            CameraManager cameraManager = getSystemService(CameraManager.class);
            String[] ids = cameraManager.getCameraIdList();
            Log.d(TAG, "camera ids: {\"" + StringUtils.joinWith("\", \"", ids) + "\"}");
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "No camera permissions");
                showToast("No camera permissions");
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
                return;
            }
            cameraManager.openCamera(ids[0], new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    try {
                        Log.d(TAG, "opened camera");
                        int width = 1024;
                        int height = 1024;
                        //TODO Hacky; query proper stuff
                        ImageReader ir = ImageReader.newInstance(width, height, ImageFormat.RAW_SENSOR, 1);
                        Surface surface = ir.getSurface();
                        camera.createCaptureSession(new ArrayList<Surface>(){{
                                add(surface);
                            }}, new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession session) {
                                try {
                                    Log.d(TAG, "configured camera");
                                    CaptureRequest.Builder captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                                    captureRequestBuilder.addTarget(surface);
                                    CaptureRequest captureRequest = captureRequestBuilder.build();
                                    session.capture(captureRequest, new CameraCaptureSession.CaptureCallback() {
                                        @Override
                                        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                                            Log.d(TAG, "capture completed");
                                            Image image = ir.acquireNextImage();
                                            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                                            byte[] bytes = new byte[buffer.capacity()];
                                            buffer.get(bytes);
                                            Bitmap bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
                                            imageView.setImageBitmap(bitmapImage);
                                            super.onCaptureCompleted(session, request, result);
                                        }

                                        @Override
                                        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
                                            Log.e(TAG, "Capture failed: " + failure);
                                            super.onCaptureFailed(session, request, failure);
                                        }
                                    }, null);
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                                Log.e(TAG, "Configure session failed");
                            }
                        }, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    Log.e(TAG, "Disconnected from camera");
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    Log.e(TAG, "Error opening camera: " + error);
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }


    }

    private void showToast(String msg) {
        runOnUiThread(() -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show());
    }
}
