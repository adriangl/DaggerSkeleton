package com.bq.daggerskeleton.camera.storage;

import android.hardware.camera2.TotalCaptureResult;

import java.util.Arrays;

public class StorageState {
    public TotalCaptureResult pendingCaptureTotalResult;
    public byte[] pendingCaptureBytes;

    public StorageState() {

    }

    public StorageState(StorageState other) {
        this.pendingCaptureTotalResult = other.pendingCaptureTotalResult;
        this.pendingCaptureBytes = other.pendingCaptureBytes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StorageState that = (StorageState) o;

        if (pendingCaptureTotalResult != null ? !pendingCaptureTotalResult.equals(that.pendingCaptureTotalResult) : that.pendingCaptureTotalResult != null)
            return false;
        return Arrays.equals(pendingCaptureBytes, that.pendingCaptureBytes);

    }

    @Override
    public int hashCode() {
        int result = pendingCaptureTotalResult != null ? pendingCaptureTotalResult.hashCode() : 0;
        result = 31 * result + Arrays.hashCode(pendingCaptureBytes);
        return result;
    }
}
