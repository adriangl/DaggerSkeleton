package com.bq.daggerskeleton;

import android.app.Application;

import com.bq.daggerskeleton.camera.InitAction;
import com.bq.daggerskeleton.flux.Dispatcher;
import com.bq.daggerskeleton.flux.Store;

import java.util.ArrayList;

public class App extends Application {

    private AppComponent appComponent;

    @Override
    public void onCreate() {
        super.onCreate();
        appComponent = DaggerAppComponent.builder()
                .module(new AppComponent.Module(this))
                .build();
        ArrayList<Store<?>> stores = new ArrayList<>(appComponent.stores());

        // Initialize stuff
        Dispatcher.dispatch(new InitAction());
    }


    public AppComponent getAppComponent() {
        return appComponent;
    }
}
