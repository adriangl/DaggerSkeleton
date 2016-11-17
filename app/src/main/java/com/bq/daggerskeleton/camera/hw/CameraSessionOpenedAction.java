package com.bq.daggerskeleton.camera.hw;

import android.hardware.camera2.CameraCaptureSession;

import com.bq.daggerskeleton.flux.Action;

public class CameraSessionOpenedAction implements Action {
    public final CameraCaptureSession cameraSession;

    public CameraSessionOpenedAction(CameraCaptureSession cameraSession) {
        this.cameraSession = cameraSession;
    }

    @Override
    public String toString() {
        return "CameraSessionOpenedAction{" +
                "cameraSession=" + cameraSession +
                '}';
    }
}
