package com.bq.daggerskeleton.camera.hw;

import android.view.Surface;

import com.bq.daggerskeleton.flux.Action;

public class CaptureTargetSurfaceReadyAction implements Action {
    public final Surface targetSurface;

    public CaptureTargetSurfaceReadyAction(Surface targetSurface) {
        this.targetSurface = targetSurface;
    }

    @Override
    public String toString() {
        return "CaptureTargetSurfaceReadyAction{" +
                "targetSurface=" + targetSurface +
                '}';
    }
}
