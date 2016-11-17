package com.bq.daggerskeleton.camera.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.FrameLayout;

import com.bq.daggerskeleton.R;
import com.bq.daggerskeleton.common.Plugin;
import com.bq.daggerskeleton.common.SimplePlugin;
import com.bq.daggerskeleton.dagger.PluginScope;

import javax.inject.Inject;

import dagger.Provides;
import dagger.multibindings.ClassKey;
import dagger.multibindings.IntoMap;

@PluginScope
public class ViewControllerPlugin extends SimplePlugin {

    private Activity activity;

    private FrameLayout cameraFrame;
    private FrameLayout shutterButtonFrame;
    private FrameLayout shutterLeftFrame;
    private FrameLayout shutterRightFrame;

    @Inject
    public ViewControllerPlugin(Activity activity) {
        this.activity = activity;
        // TODO: 10/11/16 Inflate other views here...
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cameraFrame = (FrameLayout) this.activity.findViewById(R.id.camera_frame);
        shutterButtonFrame = (FrameLayout) this.activity.findViewById(R.id.shutter_button_main_frame);
        shutterLeftFrame = (FrameLayout) this.activity.findViewById(R.id.shutter_button_left_frame);
        shutterRightFrame = (FrameLayout) this.activity.findViewById(R.id.shutter_button_right_frame);
    }

    public FrameLayout getCameraFrame() {
        return cameraFrame;
    }

    public FrameLayout getShutterButtonFrame() {
        return shutterButtonFrame;
    }

    public FrameLayout getShutterLeftFrame() {
        return shutterLeftFrame;
    }

    public FrameLayout getShutterRightFrame() {
        return shutterRightFrame;
    }


    @dagger.Module
    public static abstract class Module {
        @Provides
        @PluginScope
        @IntoMap
        @ClassKey(ViewControllerPlugin.class)
        static Plugin provideViewControllerPlugin(ViewControllerPlugin plugin) {
            return plugin;
        }
    }

}
