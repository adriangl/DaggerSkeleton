package com.bq.daggerskeleton.camera.photo;

import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.support.annotation.NonNull;
import android.util.Size;

import com.bq.daggerskeleton.AppScope;
import com.bq.daggerskeleton.camera.hw.CameraDescription;
import com.bq.daggerskeleton.camera.hw.CameraDeviceOpenedAction;
import com.bq.daggerskeleton.camera.hw.CameraMode;
import com.bq.daggerskeleton.camera.hw.CameraStore;
import com.bq.daggerskeleton.camera.hw.CaptureTargetSurfaceReadyAction;
import com.bq.daggerskeleton.camera.hw.CloseCameraAction;
import com.bq.daggerskeleton.camera.rotation.RotationStore;
import com.bq.daggerskeleton.camera.rotation.RotationUtils;
import com.bq.daggerskeleton.camera.storage.CaptureSavedAction;
import com.bq.daggerskeleton.camera.ui.SetModeAction;
import com.bq.daggerskeleton.camera.ui.TakePictureAction;
import com.bq.daggerskeleton.flux.Dispatcher;
import com.bq.daggerskeleton.flux.Store;

import java.nio.ByteBuffer;

import javax.inject.Inject;

import dagger.Provides;
import dagger.multibindings.IntoSet;
import io.reactivex.functions.Consumer;

@AppScope
public class PhotoStore extends Store<PhotoState> {

    private final CameraStore cameraStore;
    private final RotationStore rotationStore;

    @Inject
    public PhotoStore(final CameraStore cameraStore, RotationStore rotationStore) {
        this.cameraStore = cameraStore;
        this.rotationStore = rotationStore;

        Dispatcher.subscribe(CameraDeviceOpenedAction.class, new Consumer<CameraDeviceOpenedAction>() {
            @Override
            public void accept(CameraDeviceOpenedAction action) throws Exception {
                if (isInPhotoMode()) {
                    setState(createImageReader());
                    Dispatcher.dispatch(new CaptureTargetSurfaceReadyAction(state().imageReader.getSurface()));
                }
            }
        });

        Dispatcher.subscribe(TakePictureAction.class, new Consumer<TakePictureAction>() {
            @Override
            public void accept(TakePictureAction takePictureAction) throws Exception {
                if (isInPhotoMode()) {
                    PhotoState newState = new PhotoState(state());
                    newState.isTakingPhoto = true;

                    setState(newState);

                    takePicture();
                }
            }
        });

        Dispatcher.subscribe(CaptureSavedAction.class, new Consumer<CaptureSavedAction>() {
            @Override
            public void accept(CaptureSavedAction captureSavedAction) throws Exception {
                if (isInPhotoMode()) {
                    PhotoState newState = new PhotoState(state());
                    newState.isTakingPhoto = false;

                    setState(newState);
                }
            }
        });

        Dispatcher.subscribe(CloseCameraAction.class, new Consumer<CloseCameraAction>() {
            @Override
            public void accept(CloseCameraAction action) throws Exception {
                if (isInPhotoMode()) {
                    setState(releaseImageReader());
                }
            }
        });


        Dispatcher.subscribe(SetModeAction.class, new Consumer<SetModeAction>() {
            @Override
            public void accept(SetModeAction action) throws Exception {
                // If mode is not photo, then release all resources
                if (!CameraMode.PHOTO.equals(action.mode)) {
                    setState(releaseImageReader());
                }
            }
        });
    }

    @Override
    protected PhotoState initialState() {
        return new PhotoState();
    }

    private boolean isInPhotoMode() {
        return CameraMode.PHOTO.equals(cameraStore.state().cameraMode);
    }

    @NonNull
    private PhotoState createImageReader() {
        Size resolution = cameraStore.state().cameraDescription.jpegOutputSizes.get(0);

        ImageReader imageReader = ImageReader.newInstance(
                resolution.getWidth(),
                resolution.getHeight(),
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

        PhotoState newState = new PhotoState(state());
        newState.imageReader = imageReader;
        return newState;
    }

    private void takePicture() {
        try {
            final CaptureRequest.Builder jpegRequest = cameraStore.state().cameraDevice
                    .createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);

            // Render to our image reader; should be the same as the one set in the session
            jpegRequest.addTarget(state().imageReader.getSurface());
            // Configure auto-focus (AF) and auto-exposure (AE) modes:
            jpegRequest.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_AUTO);
            jpegRequest.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_ANTIBANDING_MODE_AUTO);
            // Get rotation from state
            jpegRequest.set(CaptureRequest.JPEG_ORIENTATION,
                    RotationUtils.getSensorDeviceOrientationCompensation(
                            cameraStore.state().cameraDescription.facing == CameraDescription.Facing.FRONT,
                            rotationStore.state().deviceAbsoluteRotation,
                            cameraStore.state().cameraDescription.sensorOrientation));
            // Full quality
            jpegRequest.set(CaptureRequest.JPEG_QUALITY, (byte) 100);

            cameraStore.state().cameraSession.capture(jpegRequest.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Dispatcher.dispatch(new CaptureCompletedAction(result));
                }

                @Override
                public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
                    super.onCaptureFailed(session, request, failure);
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private PhotoState releaseImageReader() {
        if (state().imageReader != null) {
            state().imageReader.close();
        }
        return initialState();
    }

    @dagger.Module
    public static class Module {
        @Provides
        @AppScope
        @IntoSet
        static Store<?> providePhotoStore(PhotoStore impl) {
            return impl;
        }
    }
}
