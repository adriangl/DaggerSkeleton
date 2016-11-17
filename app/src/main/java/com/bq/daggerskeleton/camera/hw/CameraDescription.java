package com.bq.daggerskeleton.camera.hw;

import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.support.annotation.Nullable;
import android.util.Size;

import com.bq.daggerskeleton.camera.preview.PreviewUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static android.hardware.camera2.CameraCharacteristics.LENS_FACING;
import static android.hardware.camera2.CameraCharacteristics.SENSOR_ORIENTATION;
import static android.hardware.camera2.CameraMetadata.LENS_FACING_BACK;
import static android.hardware.camera2.CameraMetadata.LENS_FACING_EXTERNAL;
import static android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT;

public class CameraDescription {

    public enum Facing {
        BACK,
        FRONT,
        EXTERNAL
    }

    public final String id;
    public final Facing facing;
    public final List<Size> jpegOutputSizes;
    public final int sensorOrientation;

    public CameraDescription(String id, Facing facing, List<Size> jpegOutputSizes, int sensorOrientation) {
        this.id = id;
        this.facing = facing;
        this.jpegOutputSizes = jpegOutputSizes;
        this.sensorOrientation = sensorOrientation;
    }

    public static CameraDescription from(String id, CameraCharacteristics cameraCharacteristics) {
        List<Size> jpegResolutions = getJpegOutputSizes(cameraCharacteristics);
        Facing facing = getCameraFacing(cameraCharacteristics);
        int sensorOrientation = getSensorOrientation(cameraCharacteristics);

        return new CameraDescription(id, facing, jpegResolutions, sensorOrientation);
    }

    @Override
    public String toString() {
        return "CameraDescription{" +
                "id='" + id + '\'' +
                ", facing=" + facing +
                ", jpegOutputSizes=" + jpegOutputSizes +
                ", sensorOrientation=" + sensorOrientation +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CameraDescription that = (CameraDescription) o;

        if (sensorOrientation != that.sensorOrientation) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (facing != that.facing) return false;
        return jpegOutputSizes != null ? jpegOutputSizes.equals(that.jpegOutputSizes) : that.jpegOutputSizes == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (facing != null ? facing.hashCode() : 0);
        result = 31 * result + (jpegOutputSizes != null ? jpegOutputSizes.hashCode() : 0);
        result = 31 * result + sensorOrientation;
        return result;
    }

    @Nullable
    private static List<Size> getJpegOutputSizes(CameraCharacteristics cameraCharacteristics) {
        // Get JPEG resolutions
        Size[] jpegResolutionsArray = null;

        StreamConfigurationMap streamConfigurationMap = cameraCharacteristics
                .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        if (streamConfigurationMap != null) {
            jpegResolutionsArray = streamConfigurationMap.getOutputSizes(ImageFormat.JPEG);
        }
        List<Size> jpegResolutions = Arrays.asList(jpegResolutionsArray);

        // Sort by descending area size
        Collections.sort(jpegResolutions, new PreviewUtil.CompareSizesByArea());
        Collections.reverse(jpegResolutions);

        return jpegResolutions;
    }

    private static Facing getCameraFacing(CameraCharacteristics cameraCharacteristics) {
        switch (cameraCharacteristics.get(LENS_FACING)) {
            case LENS_FACING_BACK:
                return Facing.BACK;
            case LENS_FACING_FRONT:
                return Facing.FRONT;
            case LENS_FACING_EXTERNAL:
                return Facing.EXTERNAL;
            default:
                throw new RuntimeException("Camera not supported");
        }
    }

    private static int getSensorOrientation(CameraCharacteristics cameraCharacteristics) {
        return cameraCharacteristics.get(SENSOR_ORIENTATION);
    }
}
