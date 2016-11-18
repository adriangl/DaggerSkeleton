package com.bq.daggerskeleton.camera.ui;

import com.bq.daggerskeleton.camera.hw.CameraMode;
import com.bq.daggerskeleton.flux.Action;

public class SetModeAction implements Action {

    public final CameraMode mode;

    public SetModeAction(CameraMode mode) {
        this.mode = mode;
    }

    @Override
    public String toString() {
        return "SwitchModeAction{" +
                "mode=" + mode +
                '}';
    }
}
