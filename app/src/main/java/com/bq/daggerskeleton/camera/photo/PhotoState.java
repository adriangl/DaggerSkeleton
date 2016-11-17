package com.bq.daggerskeleton.camera.photo;

import android.media.ImageReader;

public class PhotoState {
    public ImageReader imageReader;
    public boolean isTakingPhoto;

    public PhotoState(PhotoState other) {
        this.imageReader = other.imageReader;
        this.isTakingPhoto = other.isTakingPhoto;
    }

    public PhotoState() {

    }

    @Override
    public String toString() {
        return "PhotoState{" +
                "imageReader=" + imageReader +
                ", isTakingPhoto=" + isTakingPhoto +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PhotoState that = (PhotoState) o;

        if (isTakingPhoto != that.isTakingPhoto) return false;
        return imageReader != null ? imageReader.equals(that.imageReader) : that.imageReader == null;

    }

    @Override
    public int hashCode() {
        int result = imageReader != null ? imageReader.hashCode() : 0;
        result = 31 * result + (isTakingPhoto ? 1 : 0);
        return result;
    }
}
