package com.bq.daggerskeleton.camera.hw;

import android.hardware.camera2.CameraDevice;

import com.bq.daggerskeleton.flux.Action;

public class CameraErrorAction implements Action {
    private final CameraDevice camera;

    public CameraErrorAction(CameraDevice camera) {
        this.camera = camera;
    }

    @Override
    public String toString() {
        return "CameraErrorAction{" +
                "camera=" + camera +
                '}';
    }
}
