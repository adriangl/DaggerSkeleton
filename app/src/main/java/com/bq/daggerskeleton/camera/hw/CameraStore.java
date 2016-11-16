package com.bq.daggerskeleton.camera.hw;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;

import com.bq.daggerskeleton.App;
import com.bq.daggerskeleton.AppScope;
import com.bq.daggerskeleton.camera.InitAction;
import com.bq.daggerskeleton.camera.permissions.PermissionChangedAction;
import com.bq.daggerskeleton.camera.preview.CameraPreviewAvailableAction;
import com.bq.daggerskeleton.camera.preview.CameraPreviewDestroyedAction;
import com.bq.daggerskeleton.camera.rotation.RotationStore;
import com.bq.daggerskeleton.camera.ui.TakePictureAction;
import com.bq.daggerskeleton.flux.Dispatcher;
import com.bq.daggerskeleton.flux.Store;

import java.nio.ByteBuffer;
import java.util.Arrays;

import javax.inject.Inject;

import dagger.Provides;
import dagger.multibindings.IntoSet;
import io.reactivex.functions.Consumer;

import static com.bq.daggerskeleton.camera.preview.AutoFitTextureView.PREVIEW_4_3;

@AppScope
public class CameraStore extends Store<CameraState> {

    private final CameraManager cameraManager;
    private final RotationStore rotationStore;

    @Inject
    public CameraStore(App app, RotationStore rotationStore) {
        this.cameraManager = (CameraManager) app.getSystemService(Context.CAMERA_SERVICE);
        this.rotationStore = rotationStore;

        // Init needed variables
        Dispatcher.subscribe(InitAction.class, new Consumer<InitAction>() {
            @Override
            public void accept(InitAction initAction) throws Exception {
                setState(getInitialCameraParameters());
            }
        });

        // If the user granted camera permissions, then proceed with opening
        // the camera device and dispatch the proper action
        Dispatcher.subscribe(PermissionChangedAction.class, new Consumer<PermissionChangedAction>() {
            @Override
            public void accept(PermissionChangedAction action) throws Exception {
                CameraState newState = new CameraState(state());
                newState.hasCameraPermission = action.granted;
                setState(newState);
            }
        });

        Dispatcher.subscribe(OpenCameraAction.class, new Consumer<OpenCameraAction>() {
            @Override
            public void accept(OpenCameraAction action) throws Exception {
                if (!state().hasCameraPermission) return;
                if (state().cameraId == null) return;

                //noinspection MissingPermission
                cameraManager.openCamera(state().cameraId, new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(CameraDevice camera) {
                        ImageReader imageReader = createImageReader();
                        Dispatcher.dispatch(new CameraDeviceOpenedAction(camera, imageReader));
                    }

                    @Override
                    public void onDisconnected(CameraDevice camera) {
                        Log.e("CameraStore", "openCamera disconnected");
                    }

                    @Override
                    public void onError(CameraDevice camera, int error) {
                        Log.e("CameraStore", String.format("openCamera error: %d", error));
                    }
                }, null);
            }
        });

        // If a camera preview is available, try to open a camera session for preview
        Dispatcher.subscribe(CameraPreviewAvailableAction.class, new Consumer<CameraPreviewAvailableAction>() {
            @Override
            public void accept(CameraPreviewAvailableAction action) throws Exception {
                CameraState newState = new CameraState(state());
                newState.previewSurface = action.previewSurface;
                setState(newState);

                tryToOpenSession();
            }
        });

        // If a camera device is opened, try to open a camera session for preview
        Dispatcher.subscribe(CameraDeviceOpenedAction.class, new Consumer<CameraDeviceOpenedAction>() {
            @Override
            public void accept(CameraDeviceOpenedAction action) throws Exception {
                CameraState newState = new CameraState(state());
                newState.cameraDevice = action.cameraDevice;
                newState.imageReader = action.imageReader;
                setState(newState);

                tryToOpenSession();
            }
        });

        // Do whatever you want when the session is created
        Dispatcher.subscribe(CameraSessionOpenedAction.class, new Consumer<CameraSessionOpenedAction>() {
            @Override
            public void accept(CameraSessionOpenedAction action) throws Exception {
                setState(configSessionForPreview(action.cameraSession));
            }
        });

        // Listen to camera shutter events so we can take a picture
        Dispatcher.subscribe(TakePictureAction.class, new Consumer<TakePictureAction>() {
            @Override
            public void accept(TakePictureAction action) throws Exception {
                // Configure camera for captures
                // TODO: 15/11/16 Change state (How? dunno...)
                takePicture();
            }
        });

        // Release camera resources when the camera preview is destroyed and reset state to initial state
        Dispatcher.subscribe(CameraPreviewDestroyedAction.class, new Consumer<CameraPreviewDestroyedAction>() {
            @Override
            public void accept(CameraPreviewDestroyedAction action) throws Exception {
                setState(releaseCameraResources());
            }
        });

        // Release camera resources when the a close action is performed
        Dispatcher.subscribe(CloseCameraAction.class, new Consumer<CloseCameraAction>() {
            @Override
            public void accept(CloseCameraAction action) throws Exception {
                setState(releaseCameraResources());
            }
        });
    }

    @Override
    public CameraState initialState() {
        CameraState initialState = new CameraState();
        initialState.previewAspectRatio = PREVIEW_4_3;
        return initialState;
    }

    private CameraState getInitialCameraParameters() {

        try {
            String mainCameraId = cameraManager.getCameraIdList()[0];
            Size[] photoSizes = null;

            StreamConfigurationMap streamConfigurationMap = cameraManager.getCameraCharacteristics(mainCameraId)
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            if (streamConfigurationMap != null) {
                photoSizes = streamConfigurationMap.getOutputSizes(ImageFormat.JPEG);
            }

            CameraState newState = new CameraState(state());
            newState.cameraId = mainCameraId;
            newState.cameraResolutionList = photoSizes;

            return newState;

        } catch (CameraAccessException e) {
            e.printStackTrace();
            return state();
        }

    }

    @NonNull
    private ImageReader createImageReader() {
        // TODO: 15/11/16 Where the f*** should I create the ImageReader?
        ImageReader imageReader = ImageReader.newInstance(
                state().cameraResolutionList[0].getWidth(),
                state().cameraResolutionList[0].getHeight(),
                ImageFormat.JPEG,
                2);

        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                try (final Image image = reader.acquireLatestImage()) {
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);
                    Dispatcher.dispatch(new CaptureBytesTakenAction(bytes));
                }
            }
        }, null);

        return imageReader;
    }

    private void tryToOpenSession() {
        if (state().cameraSession != null)
            return; // TODO: 15/11/16 Maybe we should open a new session if this is marked as closed or invalid, rather than checking for null values
        if (state().cameraDevice == null) return;
        if (state().previewSurface == null) return;
        if (state().imageReader == null) return;

        try {
            state().cameraDevice.createCaptureSession(
                    Arrays.asList(state().previewSurface, state().imageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            Dispatcher.dispatch(new CameraSessionOpenedAction(session));
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
                            Dispatcher.dispatch(new CameraSessionFailed(session));
                        }
                    }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private CameraState configSessionForPreview(CameraCaptureSession cameraSession) {

        try {
            CaptureRequest.Builder builder = state().cameraDevice
                    .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            // Only add preview surface so we don't spam image reader
            builder.addTarget(state().previewSurface);

            cameraSession.setRepeatingRequest(
                    builder.build(),
                    null,
                    null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        CameraState newState = new CameraState(state());
        newState.cameraSession = cameraSession;

        return newState;
    }

    private void takePicture() {
        try {
            final CaptureRequest.Builder jpegRequest = state().cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            // Render to our image reader:
            jpegRequest.addTarget(state().imageReader.getSurface());
            // Configure auto-focus (AF) and auto-exposure (AE) modes:
            jpegRequest.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_AUTO);
            jpegRequest.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_ANTIBANDING_MODE_AUTO);
            // Get rotation from state
            jpegRequest.set(CaptureRequest.JPEG_ORIENTATION, getJpegOrientation());

            state().cameraSession.capture(jpegRequest.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Dispatcher.dispatch(new CaptureCompletedAction(result));
                }

                @Override
                public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
                    super.onCaptureFailed(session, request, failure);
                    // TODO: 15/11/16 Modify state? Send action?
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private Integer getJpegOrientation() {
        try {
            CameraCharacteristics cameraCharacteristics = cameraManager
                    .getCameraCharacteristics(state().cameraId);
            boolean isFacingFront =
                    Integer.valueOf(CameraCharacteristics.LENS_FACING_FRONT).equals
                            (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING));
            int cameraDefaultOrientation = 0;
            if (cameraCharacteristics.getKeys().contains(CameraCharacteristics
                    .SENSOR_ORIENTATION)) {
                //noinspection ConstantConditions
                cameraDefaultOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            }
            // Round sensor orientation value
            int roundedOrientation = ((rotationStore.state().deviceAbsoluteRotation) + 45) / 90 * 90;
            int rotation;
            if (isFacingFront) {
                rotation = (cameraDefaultOrientation + roundedOrientation + 360) % 360;
            } else {  // back-facing camera
                rotation = (cameraDefaultOrientation - roundedOrientation) % 360;
            }
            return rotation;
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private CameraState releaseCameraResources() {
        try {
            if (state().cameraSession != null) {
                state().cameraSession.stopRepeating();
                state().cameraSession.close();
            }
            if (state().cameraDevice != null) {
                state().cameraDevice.close();
            }
            if (state().imageReader != null) {
                state().imageReader.close();
            }
            if (state().previewSurface != null) {
                state().previewSurface.release();
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        CameraState newState = new CameraState(initialState());
        newState.hasCameraPermission = state().hasCameraPermission;
        newState.previewAspectRatio = state().previewAspectRatio;
        newState.cameraId = state().cameraId;
        newState.cameraResolutionList = state().cameraResolutionList;

        return newState;
    }

    @dagger.Module
    public static abstract class Module {
        @Provides
        @AppScope
        @IntoSet
        static Store<?> provideCameraStore(CameraStore impl) {
            return impl;
        }
    }

}
