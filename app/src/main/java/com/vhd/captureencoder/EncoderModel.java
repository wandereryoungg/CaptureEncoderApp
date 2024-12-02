package com.vhd.captureencoder;

import android.util.Log;
import android.view.SurfaceView;

import java.util.concurrent.LinkedBlockingQueue;

public class EncoderModel {

    private String TAG = getClass().getSimpleName();

    protected int QUEUE_SIZE = 8;
    private int mWidth;
    private int mHeight;
    private int mFps;
    private String mDecoderName = "c2.rk.hevc.decoder";
    protected LinkedBlockingQueue<FrameData> mFrameQueueIn;
    private SurfaceView mSurfaceView;
    private Player player;

    public EncoderModel(SurfaceView surfaceView, int width, int height, int fps) {
        mSurfaceView = surfaceView;
        mWidth = width;
        mHeight = height;
        mFps = fps;
        mFrameQueueIn = new LinkedBlockingQueue<>(QUEUE_SIZE);
        Log.i(TAG, "mWidth: " + mWidth + " mHeight: " + mHeight + " mFps: " + mFps);
    }

    public void startPlayer() {
        player = new Player(mSurfaceView, "video/hevc", mDecoderName, mWidth, mHeight, 0, mFps, mFrameQueueIn, null);
        player.startThread();
    }

    public void stopPlayer() {
        player.stopThread();
    }

    public void onGetVideoFrame(byte[] bytes, int length) {
        Log.i(TAG, "mSurfaceView" + mSurfaceView + " onGetVideoFrame bytes:" + bytes.length + "length: " + length);

        FrameData frameData = mFrameQueueIn.poll();
        if (frameData == null) {
            frameData = new FrameData();
        }

        frameData.mTimestamp = System.currentTimeMillis();
        frameData.len = bytes.length;
        frameData.mData = bytes;

        if (!mFrameQueueIn.offer(frameData)) {
            Log.e(TAG, "offer failed");
        }
    }

}
