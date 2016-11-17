package com.bq.daggerskeleton.camera.hw;

import android.hardware.camera2.TotalCaptureResult;

import com.bq.daggerskeleton.flux.Action;

public class CaptureCompletedAction implements Action {
    public final TotalCaptureResult result;

    public CaptureCompletedAction(TotalCaptureResult result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "CaptureCompletedAction{" +
                "result=" + result +
                '}';
    }
}
