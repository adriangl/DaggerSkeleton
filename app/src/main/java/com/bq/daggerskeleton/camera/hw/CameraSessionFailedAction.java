package com.bq.daggerskeleton.camera.hw;

import android.hardware.camera2.CameraCaptureSession;

import com.bq.daggerskeleton.flux.Action;

public class CameraSessionFailedAction implements Action {
    private CameraCaptureSession session;

    public CameraSessionFailedAction(CameraCaptureSession session) {
        this.session = session;
    }

    @Override
    public String toString() {
        return "CameraSessionFailedAction{" +
                "session=" + session +
                '}';
    }
}
