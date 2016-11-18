package com.bq.daggerskeleton.camera.storage;

import com.bq.daggerskeleton.flux.Action;

public class MediaSavedAction implements Action {
    public final String newFileUri;

    public MediaSavedAction(String newFileUri) {
        this.newFileUri = newFileUri;
    }

    @Override
    public String toString() {
        return "CaptureSavedAction{" +
                "newFileUri='" + newFileUri + '\'' +
                '}';
    }
}
