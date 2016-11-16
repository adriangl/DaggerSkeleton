package com.bq.daggerskeleton.camera.preview;

import com.bq.daggerskeleton.flux.Action;

public class CameraPreviewSizeChangedAction implements Action {
    public final int width;
    public final int height;

    public CameraPreviewSizeChangedAction(int width, int height) {
        this.width = width;
        this.height = height;
    }
}
