package com.bq.daggerskeleton.camera.hw;

import android.hardware.camera2.CameraDevice;

import com.bq.daggerskeleton.flux.Action;

public class CameraDeviceOpenedAction implements Action {
    public final CameraDevice cameraDevice;

    public CameraDeviceOpenedAction(CameraDevice cameraDevice) {
        this.cameraDevice = cameraDevice;
    }

    @Override
    public String toString() {
        return "CameraDeviceOpenedAction{" +
                "cameraDevice=" + cameraDevice +
                '}';
    }
}
