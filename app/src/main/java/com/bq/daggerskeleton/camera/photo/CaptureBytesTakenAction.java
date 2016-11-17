package com.bq.daggerskeleton.camera.photo;

import com.bq.daggerskeleton.flux.Action;

public class CaptureBytesTakenAction implements Action {
    public final byte[] imageBytes;

    public CaptureBytesTakenAction(byte[] imageBytes) {
        this.imageBytes = imageBytes;
    }

    @Override
    public String toString() {
        return "CaptureBytesTakenAction{" +
                "imageBytes=[" + imageBytes.length + "]" +
                '}';
    }
}