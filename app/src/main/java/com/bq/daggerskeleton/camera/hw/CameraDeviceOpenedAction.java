package com.bq.daggerskeleton.camera.hw;

import android.hardware.camera2.CameraDevice;
import android.media.ImageReader;

import com.bq.daggerskeleton.flux.Action;

public class CameraDeviceOpenedAction implements Action {
    public final CameraDevice cameraDevice;
    public final ImageReader imageReader;

    public CameraDeviceOpenedAction(CameraDevice cameraDevice, ImageReader imageReader) {
        this.cameraDevice = cameraDevice;
        this.imageReader = imageReader;
    }

    @Override
    public String toString() {
        return "CameraDeviceOpenedAction{" +
                "cameraDevice=" + cameraDevice +
                ", imageReader=" + imageReader +
                '}';
    }
}
