package com.bq.daggerskeleton.camera.preview;

import android.view.Surface;

import com.bq.daggerskeleton.flux.Action;

public class CameraPreviewAvailableAction implements Action {
    public final Surface previewSurface;
    public final int previewWidth;
    public final int previewHeight;


    public CameraPreviewAvailableAction(Surface previewSurface, int previewWidth, int previewHeight) {
        this.previewSurface = previewSurface;
        this.previewWidth = previewWidth;
        this.previewHeight = previewHeight;
    }
}
