package com.vhd.captureencoder;

import android.util.Log;
import android.view.Surface;

import java.util.ArrayList;
import java.util.List;

public class CaptureModel {

    private String TAG = getClass().getSimpleName();

    private int mCameraId;
    private int mWidth;
    private int mHeight;
    private int mFps;
    private Surface mSurface;
    private List<Integer> encoderIds = new ArrayList<>();

    public CaptureModel(int cameraId, int width, int height, int fps) {
        Log.e(TAG, "CaptureModel create");
        this.mCameraId = cameraId;
        this.mWidth = width;
        this.mHeight = height;
        this.mFps = fps;
        nativeInit(mCameraId);
    }

    public void release() {
        nativeRelease(mCameraId);
    }

    public void updateParams(Surface surface, int width, int height, int fps) {
        mSurface = surface;
        this.mWidth = width;
        this.mHeight = height;
        this.mFps = fps;
    }

    public int startCapture() {
        int ret = startCapture(mCameraId, mSurface, mWidth, mHeight, mFps);
        Log.i(TAG, "startCapture ret: " + ret);
        return ret;
    }

    public int stopCapture() {
        int ret = stopCapture(mCameraId);
        Log.i(TAG, "stopCapture ret: " + ret);
        return ret;
    }

    public void addEncoder(EncoderModel encoderModel) {
        int id = addEncoder(mCameraId, encoderModel);
        encoderIds.add(id);
    }

    public void removeEncoder(int encoderId) {
        removeEncoder(mCameraId, encoderId);
    }

    public void removeAllEncoder() {
        for (Integer id : encoderIds) {
            removeEncoder(mCameraId, id);
        }
        encoderIds.clear();
    }

    private native void nativeInit(int cameraId);

    private native int nativeRelease(int cameraId);

    private native int startCapture(int cameraId, Surface surface, int width, int height, int fps);

    private native int stopCapture(int cameraId);

    private native int addEncoder(int cameraId, EncoderModel encoderModel);

    private native int removeEncoder(int cameraId, int encoderId);

}
