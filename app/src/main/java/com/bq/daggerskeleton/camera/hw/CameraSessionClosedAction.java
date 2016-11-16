package com.bq.daggerskeleton.camera.hw;

import android.hardware.camera2.CameraCaptureSession;

import com.bq.daggerskeleton.flux.Action;

public class CameraSessionClosedAction implements Action {
    public final CameraCaptureSession cameraSession;

    public CameraSessionClosedAction(CameraCaptureSession cameraSession) {
        this.cameraSession = cameraSession;
    }
}
