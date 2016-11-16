package com.bq.daggerskeleton.camera.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

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


public class ShutterButtonPlugin extends SimplePlugin implements View.OnClickListener {

    private final Context context;
    private final ViewControllerPlugin viewControllerPlugin;
    private final CameraStore cameraStore;

    private Button shutterButton;
    private FrameLayout.LayoutParams shutterButtonParams;

    @Inject
    public ShutterButtonPlugin(Activity activity,
                               ViewControllerPlugin viewControllerPlugin,
                               CameraStore cameraStore) {
        this.context = activity;
        this.viewControllerPlugin = viewControllerPlugin;
        this.cameraStore = cameraStore;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        shutterButton = new Button(context);
        shutterButtonParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        shutterButton.setOnClickListener(this);
    }

    @Override
    public void onPluginsCreated() {
        super.onPluginsCreated();
        viewControllerPlugin.getShutterButtonFrame().addView(shutterButton, shutterButtonParams);
    }

    @Override
    public void onResume() {
        super.onResume();
        track(cameraStore.flowable()
                .subscribe(new Consumer<CameraState>() {
                    @Override
                    public void accept(CameraState cameraState) throws Exception {
                        // TODO: 15/11/16 enable or disable button when needed
                    }
                }));
    }

    @Override
    public void onClick(View v) {
        Dispatcher.dispatch(new TakePictureAction());
    }


    @dagger.Module
    public static abstract class Module {
        @Provides
        @PluginScope
        @IntoMap
        @ClassKey(ShutterButtonPlugin.class)
        static Plugin provideShutterButtonPlugin(ShutterButtonPlugin plugin) {
            return plugin;
        }
    }
}
