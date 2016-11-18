package com.bq.daggerskeleton.camera.hw;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.util.Log;

import com.bq.daggerskeleton.App;
import com.bq.daggerskeleton.AppScope;
import com.bq.daggerskeleton.camera.InitAction;
import com.bq.daggerskeleton.camera.permissions.PermissionChangedAction;
import com.bq.daggerskeleton.camera.preview.CameraPreviewAvailableAction;
import com.bq.daggerskeleton.camera.preview.CameraPreviewDestroyedAction;
import com.bq.daggerskeleton.camera.ui.SetModeAction;
import com.bq.daggerskeleton.flux.Dispatcher;
import com.bq.daggerskeleton.flux.Store;

import java.util.Arrays;

import javax.inject.Inject;

import dagger.Provides;
import dagger.multibindings.IntoSet;
import io.reactivex.functions.Consumer;

import static com.bq.daggerskeleton.camera.preview.AutoFitTextureView.PREVIEW_4_3;

@AppScope
public class CameraStore extends Store<CameraState> {

    private final CameraManager cameraManager;

    @Inject
    public CameraStore(App app) {
        this.cameraManager = (CameraManager) app.getSystemService(Context.CAMERA_SERVICE);

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

        // Received an action to open the camera in the needed mode
        Dispatcher.subscribe(OpenCameraAction.class, new Consumer<OpenCameraAction>() {
            @Override
            public void accept(OpenCameraAction action) throws Exception {
                if (!state().hasCameraPermission) return;
                if (state().cameraDescription == null) return;

                //noinspection MissingPermission
                cameraManager.openCamera(state().cameraDescription.id, new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(CameraDevice camera) {
                        Dispatcher.dispatch(new CameraDeviceOpenedAction(camera));
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

        // When a target surface is available, try to open the session
        Dispatcher.subscribe(CaptureTargetSurfaceReadyAction.class, new Consumer<CaptureTargetSurfaceReadyAction>() {
            @Override
            public void accept(CaptureTargetSurfaceReadyAction action) throws Exception {
                CameraState newState = new CameraState(state());
                newState.targetSurface = action.targetSurface;
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
                setState(newState);

                tryToOpenSession();
            }
        });

        // Do whatever you want when the session is created
        Dispatcher.subscribe(CameraSessionOpenedAction.class, new Consumer<CameraSessionOpenedAction>() {
            @Override
            public void accept(CameraSessionOpenedAction action) throws Exception {
                CameraState newState = new CameraState(state());
                newState.cameraSession = action.cameraSession;
                setState(newState);

                configSessionForPreview();
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


        // Save current camera mode in state
        Dispatcher.subscribe(SetModeAction.class, new Consumer<SetModeAction>() {
            @Override
            public void accept(SetModeAction action) throws Exception {
                CameraState newState = new CameraState(state());
                newState.cameraMode = action.mode;

                setState(newState);

                // Restart session and reload
                Dispatcher.dispatch(new RestartCameraSessionAction());
            }
        });

        Dispatcher.subscribe(RestartCameraSessionAction.class, new Consumer<RestartCameraSessionAction>() {
            @Override
            public void accept(RestartCameraSessionAction restartCameraSessionAction) throws Exception {
                setState(releaseCameraSession(state()));

                // TODO: 18/11/16 Check if we should rename/alter this action
                Dispatcher.dispatch(new CameraDeviceOpenedAction(state().cameraDevice));
            }
        });
    }

    @Override
    protected CameraState initialState() {
        CameraState initialState = new CameraState();
        initialState.previewAspectRatio = PREVIEW_4_3;
        return initialState;
    }

    private CameraState getInitialCameraParameters() {

        try {
            String mainCameraId = cameraManager.getCameraIdList()[0];

            CameraState newState = new CameraState(state());
            newState.cameraDescription = CameraDescription.from(mainCameraId, cameraManager.getCameraCharacteristics(mainCameraId));

            // TODO: 17/11/16 Get this from database, maybe?
            newState.cameraMode = CameraMode.PHOTO;

            return newState;

        } catch (CameraAccessException e) {
            e.printStackTrace();
            return state();
        }

    }

    private void tryToOpenSession() {
        if (state().cameraSession != null) return;

        if (state().cameraDevice == null) return;
        if (state().previewSurface == null) return;
        if (state().targetSurface == null) return;

        try {
            state().cameraDevice.createCaptureSession(
                    Arrays.asList(state().previewSurface, state().targetSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            Dispatcher.dispatch(new CameraSessionOpenedAction(session));
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
                            Dispatcher.dispatch(new CameraSessionFailedAction(session));
                        }
                    }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void configSessionForPreview() {
        if (state().previewSurface == null)
            return; // TODO: 17/11/16 Should we return an exception here?
        if (state().cameraDevice == null)
            return; // TODO: 17/11/16 Should we return an exception here?
        if (state().cameraSession == null)
            return; // TODO: 17/11/16 Should we return an exception here?

        try {
            CaptureRequest.Builder builder = state().cameraDevice
                    .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            // Only add preview surface so we don't spam image reader
            builder.addTarget(state().previewSurface);

            state().cameraSession.setRepeatingRequest(
                    builder.build(),
                    null,
                    null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private CameraState releaseCameraSession(CameraState state) {
        CameraState newState = new CameraState(state);
        try {
            // Release session
            if (state.cameraSession != null) {
                state.cameraSession.stopRepeating();
                state.cameraSession.close();
                newState.cameraSession = null;
            }
            // Release target surface (ImageReader surface, MediaRecorder surface...)
            if (state().targetSurface != null) {
                state().targetSurface.release();
                newState.targetSurface = null;
            }
        } catch (CameraAccessException | IllegalStateException e) {
            e.printStackTrace();
            // TODO: 17/11/16 Do something with the exception?
        }
        return newState;
    }

    private CameraState releaseCameraResources() {
        CameraState newState = new CameraState(initialState());

        // Release camera session
        newState = releaseCameraSession(newState);

        // Release camera device && related surfaces
        try {
            if (state().cameraDevice != null) {
                state().cameraDevice.close();
            }
            if (state().previewSurface != null) {
                state().previewSurface.release();
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
            // TODO: 17/11/16 Do something with the exception?
        }


        newState.hasCameraPermission = state().hasCameraPermission;
        newState.previewAspectRatio = state().previewAspectRatio;
        newState.cameraDescription = state().cameraDescription;

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
