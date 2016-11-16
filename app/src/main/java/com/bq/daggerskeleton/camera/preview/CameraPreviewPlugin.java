package com.bq.daggerskeleton.camera.preview;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.bq.daggerskeleton.camera.hw.CameraStore;
import com.bq.daggerskeleton.camera.hw.CloseCameraAction;
import com.bq.daggerskeleton.camera.hw.OpenCameraAction;
import com.bq.daggerskeleton.camera.ui.ViewControllerPlugin;
import com.bq.daggerskeleton.common.Plugin;
import com.bq.daggerskeleton.common.SimplePlugin;
import com.bq.daggerskeleton.dagger.PluginScope;
import com.bq.daggerskeleton.flux.Dispatcher;

import java.util.Arrays;
import java.util.Collections;

import javax.inject.Inject;

import dagger.Provides;
import dagger.multibindings.ClassKey;
import dagger.multibindings.IntoMap;

@PluginScope
public class CameraPreviewPlugin extends SimplePlugin {

    private final ViewControllerPlugin viewControllerPlugin;
    private final Context context;
    private final CameraStore cameraStore;
    private AutoFitTextureView cameraPreviewView;

    @Inject
    public CameraPreviewPlugin(Context context,
                               CameraStore cameraStore,
                               ViewControllerPlugin viewControllerPlugin) {
        this.context = context;
        this.cameraStore = cameraStore;
        this.viewControllerPlugin = viewControllerPlugin;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final FrameLayout.LayoutParams cameraPreviewParams =
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        cameraPreviewView = new AutoFitTextureView(context);
        cameraPreviewView.setLayoutParams(cameraPreviewParams);

        cameraPreviewView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                configureAndDispatchTexture(surface);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                // TODO: 16/11/16 Dispatcher.dispatch(new CameraPreviewSizeChangedAction(width, height));
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                Dispatcher.dispatch(new CameraPreviewDestroyedAction());
                return true; // Returning true releases the surface automatically (we don't need to keep a still image)
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                // Nothing
            }
        });
    }

    private void configureAndDispatchTexture(SurfaceTexture surface) {
        Size bufferPreviewSize = configureTextureView();
        Dispatcher.dispatch(
                new CameraPreviewAvailableAction(
                        new Surface(surface),
                        bufferPreviewSize.getWidth(),
                        bufferPreviewSize.getHeight()));
    }

    private Size configureTextureView() {
        Size[] currentCameraResolutions = cameraStore.state().cameraResolutionList;

        Size aspectRatio = cameraStore.state().previewAspectRatio;

        Size captureSize = Collections.max(
                Arrays.asList(currentCameraResolutions),
                new PreviewUtil.CompareSizesByArea());

        Size bufferSize = PreviewUtil.chooseOptimalSize(
                currentCameraResolutions,
                cameraPreviewView.getWidth(),
                cameraPreviewView.getWidth() * (aspectRatio.getWidth() / aspectRatio.getHeight()),
                this.viewControllerPlugin.getCameraFrame().getWidth(),
                this.viewControllerPlugin.getCameraFrame().getHeight(),
                captureSize
        );

        cameraPreviewView.getSurfaceTexture().setDefaultBufferSize(bufferSize.getWidth(), bufferSize.getHeight());

        configAspectRatio();

        return bufferSize;

    }

    private void configAspectRatio() {
        Size aspectRatio = cameraStore.state().previewAspectRatio;
        cameraPreviewView.setAspectRatio(aspectRatio.getWidth(), aspectRatio.getHeight());
    }

    @Override
    public void onPluginsCreated() {
        super.onPluginsCreated();
        this.viewControllerPlugin.getCameraFrame().addView(cameraPreviewView);
    }

    @Override
    public void onResume() {
        if (cameraPreviewView.isAvailable()) {
            configureAndDispatchTexture(cameraPreviewView.getSurfaceTexture());
        }

        Dispatcher.dispatch(new OpenCameraAction());
    }

    @Override
    public void onPause() {
        Dispatcher.dispatch(new CloseCameraAction());
    }

    @dagger.Module
    public static abstract class Module {
        @Provides
        @PluginScope
        @IntoMap
        @ClassKey(CameraPreviewPlugin.class)
        static Plugin provideCameraPreviewPlugin(CameraPreviewPlugin plugin) {
            return plugin;
        }
    }

}
