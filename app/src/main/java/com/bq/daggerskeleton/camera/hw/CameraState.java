package com.bq.daggerskeleton.camera.hw;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.util.Size;
import android.view.Surface;

public class CameraState {
    public boolean hasCameraPermission;
    public Surface previewSurface;
    public Size previewAspectRatio;
    public CameraDescription cameraDescription;
    public CameraDevice cameraDevice;
    public Surface targetSurface;
    public CameraCaptureSession cameraSession;

    public CameraState() {

    }

    public CameraState(CameraState other) {
        this.hasCameraPermission = other.hasCameraPermission;
        this.previewSurface = other.previewSurface;
        this.previewAspectRatio = other.previewAspectRatio;
        this.cameraDescription = other.cameraDescription;
        this.cameraDevice = other.cameraDevice;
        this.targetSurface = other.targetSurface;
        this.cameraSession = other.cameraSession;
    }

    @Override
    public String toString() {
        return "CameraState{" +
                "hasCameraPermission=" + hasCameraPermission +
                ", previewSurface=" + previewSurface +
                ", previewAspectRatio=" + previewAspectRatio +
                ", cameraDescription=" + cameraDescription +
                ", cameraDevice=" + cameraDevice +
                ", targetSurface=" + targetSurface +
                ", cameraSession=" + cameraSession +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CameraState that = (CameraState) o;

        if (hasCameraPermission != that.hasCameraPermission) return false;
        if (previewSurface != null ? !previewSurface.equals(that.previewSurface) : that.previewSurface != null)
            return false;
        if (previewAspectRatio != null ? !previewAspectRatio.equals(that.previewAspectRatio) : that.previewAspectRatio != null)
            return false;
        if (cameraDescription != null ? !cameraDescription.equals(that.cameraDescription) : that.cameraDescription != null)
            return false;
        if (cameraDevice != null ? !cameraDevice.equals(that.cameraDevice) : that.cameraDevice != null)
            return false;
        if (targetSurface != null ? !targetSurface.equals(that.targetSurface) : that.targetSurface != null)
            return false;
        return cameraSession != null ? cameraSession.equals(that.cameraSession) : that.cameraSession == null;

    }

    @Override
    public int hashCode() {
        int result = (hasCameraPermission ? 1 : 0);
        result = 31 * result + (previewSurface != null ? previewSurface.hashCode() : 0);
        result = 31 * result + (previewAspectRatio != null ? previewAspectRatio.hashCode() : 0);
        result = 31 * result + (cameraDescription != null ? cameraDescription.hashCode() : 0);
        result = 31 * result + (cameraDevice != null ? cameraDevice.hashCode() : 0);
        result = 31 * result + (targetSurface != null ? targetSurface.hashCode() : 0);
        result = 31 * result + (cameraSession != null ? cameraSession.hashCode() : 0);
        return result;
    }
}
