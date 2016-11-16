package com.bq.daggerskeleton.camera.storage;

import com.bq.daggerskeleton.flux.Action;

public class CaptureSavedAction implements Action {
    public final String newFileUri;

    public CaptureSavedAction(String newFileUri) {
        this.newFileUri = newFileUri;
    }
}
