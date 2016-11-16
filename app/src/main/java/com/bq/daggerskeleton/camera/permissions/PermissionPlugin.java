package com.bq.daggerskeleton.camera.permissions;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;

import com.bq.daggerskeleton.common.Plugin;
import com.bq.daggerskeleton.common.SimplePlugin;
import com.bq.daggerskeleton.dagger.PluginScope;
import com.bq.daggerskeleton.flux.Dispatcher;

import javax.inject.Inject;

import dagger.Provides;
import dagger.multibindings.ClassKey;
import dagger.multibindings.IntoMap;


@PluginScope
public class PermissionPlugin extends SimplePlugin {

    private final Context context;

    @Inject
    public PermissionPlugin(Context context) {
        this.context = context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean grantedPermission =
                ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;

        Dispatcher.dispatch(new PermissionChangedAction(grantedPermission));

        if (!grantedPermission) {
            // TODO: 15/11/16 Request permissions with Android framework and expose a new PermissionChangedAction with the result
            Dispatcher.dispatch(new PermissionChangedAction(true));
        }
    }

    @dagger.Module
    public static abstract class Module {
        @Provides
        @PluginScope
        @IntoMap
        @ClassKey(PermissionPlugin.class)
        static Plugin providePermissionPlugin(PermissionPlugin plugin) {
            return plugin;
        }
    }
}
