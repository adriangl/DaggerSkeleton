package com.bq.daggerskeleton.camera.permissions;

import com.bq.daggerskeleton.flux.Action;

public class PermissionChangedAction implements Action {
    public final boolean granted;

    public PermissionChangedAction(boolean granted) {
        this.granted = granted;
    }

    @Override
    public String toString() {
        return "PermissionChangedAction{" +
                "granted=" + granted +
                '}';
    }
}
