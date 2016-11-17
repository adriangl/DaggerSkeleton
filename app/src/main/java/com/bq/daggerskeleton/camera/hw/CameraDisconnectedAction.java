package com.bq.daggerskeleton.camera.hw;

import android.hardware.camera2.CameraDevice;

import com.bq.daggerskeleton.flux.Action;

public class CameraDisconnectedAction implements Action {
    private final CameraDevice camera;

    public CameraDisconnectedAction(CameraDevice camera) {
        this.camera = camera;
    }

    @Override
    public String toString() {
        return "CameraDisconnectedAction{" +
                "camera=" + camera +
                '}';
    }
}
