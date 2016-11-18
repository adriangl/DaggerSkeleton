package com.bq.daggerskeleton;

import android.app.Application;

import com.bq.daggerskeleton.camera.hw.CameraComponent;
import com.bq.daggerskeleton.camera.hw.CameraStore;
import com.bq.daggerskeleton.camera.photo.PhotoStore;
import com.bq.daggerskeleton.camera.rotation.RotationStore;
import com.bq.daggerskeleton.camera.storage.StorageStore;
import com.bq.daggerskeleton.camera.video.VideoStore;
import com.bq.daggerskeleton.common.MainActivity;
import com.bq.daggerskeleton.flux.Store;

import java.util.Set;

import dagger.Component;
import dagger.Provides;

@Component(modules = {
        AppComponent.Module.class,
        CameraStore.Module.class,
        StorageStore.Module.class,
        RotationStore.Module.class,
        PhotoStore.Module.class,
        VideoStore.Module.class,
})
@AppScope
public interface AppComponent {

    Set<Store<?>> stores();

    CameraComponent cameraComponent(MainActivity.MainActivityModule activityModule);

    @dagger.Module
    class Module {
        private final App app;

        Module(App app) {
            this.app = app;
        }

        @Provides
        App provideApp() {
            return app;
        }

        @Provides
        Application provideApplication() {
            return app;
        }
    }

}
