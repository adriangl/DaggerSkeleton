package com.bq.daggerskeleton.camera.hw;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.media.ImageReader;
import android.util.Size;
import android.view.Surface;

import java.util.Arrays;

public class CameraState {
    public boolean hasCameraPermission;
    public Surface previewSurface;
    public Size previewAspectRatio;
    public String cameraId;
    public Size[] cameraResolutionList;
    public int rotation;
    public ImageReader imageReader;
    public CameraDevice cameraDevice;
    public CameraCaptureSession cameraSession;

    public CameraState() {

    }

    public CameraState(CameraState other) {
        this.hasCameraPermission = other.hasCameraPermission;
        this.previewSurface = other.previewSurface;
        this.previewAspectRatio = other.previewAspectRatio;
        this.cameraId = other.cameraId;
        this.cameraResolutionList = other.cameraResolutionList;
        this.rotation = other.rotation;
        this.imageReader = other.imageReader;
        this.cameraDevice = other.cameraDevice;
        this.cameraSession = other.cameraSession;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CameraState that = (CameraState) o;

        if (hasCameraPermission != that.hasCameraPermission) return false;
        if (rotation != that.rotation) return false;
        if (previewSurface != null ? !previewSurface.equals(that.previewSurface) : that.previewSurface != null)
            return false;
        if (previewAspectRatio != null ? !previewAspectRatio.equals(that.previewAspectRatio) : that.previewAspectRatio != null)
            return false;
        if (cameraId != null ? !cameraId.equals(that.cameraId) : that.cameraId != null)
            return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(cameraResolutionList, that.cameraResolutionList)) return false;
        if (imageReader != null ? !imageReader.equals(that.imageReader) : that.imageReader != null)
            return false;
        if (cameraDevice != null ? !cameraDevice.equals(that.cameraDevice) : that.cameraDevice != null)
            return false;
        return cameraSession != null ? cameraSession.equals(that.cameraSession) : that.cameraSession == null;

    }

    @Override
    public int hashCode() {
        int result = (hasCameraPermission ? 1 : 0);
        result = 31 * result + (previewSurface != null ? previewSurface.hashCode() : 0);
        result = 31 * result + (previewAspectRatio != null ? previewAspectRatio.hashCode() : 0);
        result = 31 * result + (cameraId != null ? cameraId.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(cameraResolutionList);
        result = 31 * result + rotation;
        result = 31 * result + (imageReader != null ? imageReader.hashCode() : 0);
        result = 31 * result + (cameraDevice != null ? cameraDevice.hashCode() : 0);
        result = 31 * result + (cameraSession != null ? cameraSession.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "CameraState{" +
                "hasCameraPermission=" + hasCameraPermission +
                ", previewSurface=" + previewSurface +
                ", previewAspectRatio=" + previewAspectRatio +
                ", cameraId='" + cameraId + '\'' +
                ", cameraResolutionList=" + Arrays.toString(cameraResolutionList) +
                ", rotation=" + rotation +
                ", imageReader=" + imageReader +
                ", cameraDevice=" + cameraDevice +
                ", cameraSession=" + cameraSession +
                '}';
    }
}
