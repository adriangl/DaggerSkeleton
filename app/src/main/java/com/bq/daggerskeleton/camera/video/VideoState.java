package com.bq.daggerskeleton.camera.video;

import android.media.MediaRecorder;

public class VideoState {
    public MediaRecorder mediaRecorder;
    public boolean isRecording;

    public VideoState() {

    }

    public VideoState(VideoState other) {
        this.mediaRecorder = other.mediaRecorder;
        this.isRecording = other.isRecording;
    }

    @Override
    public String toString() {
        return "VideoState{" +
                "mediaRecorder=" + mediaRecorder +
                ", isRecording=" + isRecording +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VideoState that = (VideoState) o;

        if (isRecording != that.isRecording) return false;
        return mediaRecorder != null ? mediaRecorder.equals(that.mediaRecorder) : that.mediaRecorder == null;

    }

    @Override
    public int hashCode() {
        int result = mediaRecorder != null ? mediaRecorder.hashCode() : 0;
        result = 31 * result + (isRecording ? 1 : 0);
        return result;
    }
}
