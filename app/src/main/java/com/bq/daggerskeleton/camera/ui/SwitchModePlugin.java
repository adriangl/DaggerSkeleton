package com.bq.daggerskeleton.camera.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import com.bq.daggerskeleton.camera.hw.CameraMode;
import com.bq.daggerskeleton.camera.hw.CameraState;
import com.bq.daggerskeleton.camera.hw.CameraStore;
import com.bq.daggerskeleton.common.Plugin;
import com.bq.daggerskeleton.common.SimplePlugin;
import com.bq.daggerskeleton.dagger.PluginScope;
import com.bq.daggerskeleton.flux.Dispatcher;

import javax.inject.Inject;

import dagger.Provides;
import dagger.multibindings.ClassKey;
import dagger.multibindings.IntoMap;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Predicate;

@PluginScope
public class SwitchModePlugin extends SimplePlugin implements View.OnClickListener {

    private final ViewControllerPlugin viewControllerPlugin;
    private final Activity context;
    private final CameraStore cameraStore;

    private Button switchModeButton;
    private FrameLayout.LayoutParams switchModeButtonParams;

    private CameraMode currentCameraMode;

    @Inject
    public SwitchModePlugin(Activity activity, ViewControllerPlugin viewControllerPlugin, CameraStore cameraStore) {
        this.context = activity;
        this.viewControllerPlugin = viewControllerPlugin;
        this.cameraStore = cameraStore;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        switchModeButton = new Button(context);
        switchModeButtonParams = new FrameLayout.LayoutParams(100, 100);
        switchModeButtonParams.gravity = Gravity.CENTER;
        switchModeButton.setOnClickListener(this);

        this.cameraStore.flowable()
                .filter(new Predicate<CameraState>() {
                    @Override
                    public boolean test(CameraState cameraState) throws Exception {
                        return cameraState.cameraMode != null && !cameraState.cameraMode.equals(currentCameraMode);
                    }
                })
                .subscribe(new Consumer<CameraState>() {
                    @Override
                    public void accept(CameraState cameraState) throws Exception {
                        currentCameraMode = cameraState.cameraMode;
                    }
                });
    }

    @Override
    public void onPluginsCreated() {
        super.onPluginsCreated();
        viewControllerPlugin.getShutterLeftFrame().addView(switchModeButton, switchModeButtonParams);
    }

    @Override
    public void onClick(View v) {
        Dispatcher.dispatch(new SetModeAction(
                CameraMode.PHOTO.equals(currentCameraMode) ? CameraMode.VIDEO : CameraMode.PHOTO));
    }

    @dagger.Module
    public static abstract class Module {
        @Provides
        @PluginScope
        @IntoMap
        @ClassKey(SwitchModePlugin.class)
        static Plugin provideSwitchModePlugin(SwitchModePlugin plugin) {
            return plugin;
        }
    }
}
