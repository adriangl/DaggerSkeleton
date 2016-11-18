package com.bq.daggerskeleton.camera.hw;


import com.bq.daggerskeleton.camera.permissions.PermissionPlugin;
import com.bq.daggerskeleton.camera.preview.CameraPreviewPlugin;
import com.bq.daggerskeleton.camera.ui.ShutterButtonPlugin;
import com.bq.daggerskeleton.camera.ui.SwitchModePlugin;
import com.bq.daggerskeleton.camera.ui.ViewControllerPlugin;
import com.bq.daggerskeleton.common.LoggerPlugin;
import com.bq.daggerskeleton.common.MainActivity;
import com.bq.daggerskeleton.common.Plugin;
import com.bq.daggerskeleton.dagger.PluginScope;

import java.util.Map;

import dagger.Subcomponent;

@Subcomponent(
        modules = {
                MainActivity.MainActivityModule.class,

                LoggerPlugin.LoggerModule.class,

                ViewControllerPlugin.Module.class,

                CameraPreviewPlugin.Module.class,
                PermissionPlugin.Module.class,
                ShutterButtonPlugin.Module.class,
                SwitchModePlugin.Module.class,
        }
)
@PluginScope
public interface CameraComponent {
    Map<Class<?>, Plugin> pluginMap();

    void inject(MainActivity activity);
}
