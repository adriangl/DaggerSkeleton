package com.bq.daggerskeleton.camera.video;

import com.bq.daggerskeleton.flux.Action;

import java.io.File;

public class VideoCapturedAction implements Action {
    public final File outputFile;

    public VideoCapturedAction(File outputFile) {
        this.outputFile = outputFile;
    }
}
