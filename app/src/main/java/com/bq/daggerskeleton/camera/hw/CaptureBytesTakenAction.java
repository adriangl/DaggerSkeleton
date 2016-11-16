package com.bq.daggerskeleton.camera.hw;

import com.bq.daggerskeleton.flux.Action;

public class CaptureBytesTakenAction implements Action {
    public final byte[] imageBytes;

    public CaptureBytesTakenAction(byte[] imageBytes) {
        this.imageBytes = imageBytes;
    }
}
