package com.bq.daggerskeleton.camera.hw;

import android.hardware.camera2.CameraCaptureSession;

import com.bq.daggerskeleton.flux.Action;

public class CameraSessionFailed implements Action {
    private CameraCaptureSession session;

    public CameraSessionFailed(CameraCaptureSession session) {
        this.session = session;
    }
}
