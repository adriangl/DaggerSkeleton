package com.bq.daggerskeleton.camera.video;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;
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
import com.bq.daggerskeleton.camera.ui.SetModeAction;
import com.bq.daggerskeleton.camera.ui.TakePictureAction;
import com.bq.daggerskeleton.flux.Dispatcher;
import com.bq.daggerskeleton.flux.Store;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import dagger.Provides;
import dagger.multibindings.IntoSet;
import io.reactivex.functions.Consumer;


@AppScope
public class VideoStore extends Store<VideoState> {

    private final CameraStore cameraStore;
    private final RotationStore rotationStore;

    private final File outputFile;

    @Inject
    public VideoStore(final CameraStore cameraStore, RotationStore rotationStore) {
        this.cameraStore = cameraStore;
        this.rotationStore = rotationStore;

        outputFile = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                ".video.mp4");
        outputFile.getParentFile().mkdirs();

        Dispatcher.subscribe(CameraDeviceOpenedAction.class, new Consumer<CameraDeviceOpenedAction>() {
            @Override
            public void accept(CameraDeviceOpenedAction action) throws Exception {
                if (isInVideoMode()) {
                    setState(createMediaRecorder());
                    Dispatcher.dispatch(new CaptureTargetSurfaceReadyAction(state().mediaRecorder.getSurface()));
                }
            }
        });

        Dispatcher.subscribe(TakePictureAction.class, new Consumer<TakePictureAction>() {
            @Override
            public void accept(TakePictureAction action) throws Exception {
                if (isInVideoMode()) {
                    if (!state().isRecording) {
                        setState(startVideoRecording());
                    } else {
                        setState(stopVideoRecording());
                        Dispatcher.dispatch(new VideoCapturedAction(outputFile));
                    }
                }
            }
        });

        Dispatcher.subscribe(CloseCameraAction.class, new Consumer<CloseCameraAction>() {
            @Override
            public void accept(CloseCameraAction action) throws Exception {
                if (isInVideoMode()) {
                    setState(releaseMediaRecorder());
                }
            }
        });


        Dispatcher.subscribe(SetModeAction.class, new Consumer<SetModeAction>() {
            @Override
            public void accept(SetModeAction action) throws Exception {
                // If mode is not photo, then release all resources
                if (!CameraMode.VIDEO.equals(action.mode)) {
                    setState(releaseMediaRecorder());
                }
            }
        });
    }

    private VideoState createMediaRecorder() {
        VideoState newState = new VideoState(state());

        try {
            // TODO: 18/11/16 Media configuration should be extracted from another class; besides when changing those values, the MediaRecorder must be recreated
            MediaRecorder mediaRecorder = new MediaRecorder();

            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setOutputFile(outputFile.getAbsolutePath());
            mediaRecorder.setVideoEncodingBitRate(10000000);
            mediaRecorder.setVideoFrameRate(30);
            Size videoSize = chooseVideoSize(cameraStore.state().cameraDescription.mediaRecorderOutputSizes);
            mediaRecorder.setVideoSize(videoSize.getWidth(), videoSize.getHeight());
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

            mediaRecorder.setOrientationHint(RotationUtils.getSensorDeviceOrientationCompensation(
                    cameraStore.state().cameraDescription.facing == CameraDescription.Facing.FRONT,
                    rotationStore.state().deviceAbsoluteRotation,
                    cameraStore.state().cameraDescription.sensorOrientation));
            mediaRecorder.prepare();

            newState.mediaRecorder = mediaRecorder;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return newState;
    }

    private VideoState releaseMediaRecorder() {
        VideoState newState = new VideoState(state());

        if (state().mediaRecorder != null) {
            state().mediaRecorder.release();
            newState.mediaRecorder = null;
        }

        return newState;
    }

    private VideoState startVideoRecording() {
        if (cameraStore.state().cameraDevice == null) return state();
        if (cameraStore.state().previewSurface == null) return state();

        try {
            // Stop current camera session request and create a new one for record
            cameraStore.state().cameraSession.stopRepeating();

            CaptureRequest.Builder builder = cameraStore.state().cameraDevice
                    .createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            builder.addTarget(cameraStore.state().previewSurface);
            builder.addTarget(cameraStore.state().targetSurface);

            // Set a new repeating request
            state().mediaRecorder.start();
            cameraStore.state().cameraSession.setRepeatingRequest(builder.build(), null, null);

            VideoState newState = new VideoState(state());
            newState.isRecording = true;

            return newState;

        } catch (CameraAccessException e) {
            // TODO: 18/11/16 Do something here!!
            e.printStackTrace();
            return state();
        }

    }

    private VideoState stopVideoRecording() {
        state().mediaRecorder.stop();

        VideoState newState = new VideoState(state());
        newState.mediaRecorder = null;
        newState.isRecording = false;

        return newState;
    }

    /**
     * In this sample, we choose a video size with 3x4 aspect ratio. Also, we don't use sizes
     * larger than 1080p, since MediaRecorder cannot handle such a high-resolution video.
     *
     * @param choices The list of available sizes
     * @return The video size
     */
    private static Size chooseVideoSize(List<Size> choices) {
        for (Size size : choices) {
            if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080) {
                return size;
            }
        }
        Log.e("VideoStore", "Couldn't find any suitable video size");
        return choices.get(choices.size() - 1);
    }


    @Override
    protected VideoState initialState() {
        return new VideoState();
    }

    private boolean isInVideoMode() {
        return CameraMode.VIDEO.equals(cameraStore.state().cameraMode);
    }

    @dagger.Module
    public static class Module {
        @Provides
        @AppScope
        @IntoSet
        static Store<?> provideVideoStore(VideoStore impl) {
            return impl;
        }
    }
}
