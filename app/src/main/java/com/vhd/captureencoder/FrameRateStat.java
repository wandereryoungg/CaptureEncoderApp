package com.vhd.captureencoder;

public class FrameRateStat {
    public int mIntervalSecond = 10;
    public long mLastTime = 0L;
    public int mFrameCount = 0;
    public float mRealFrameRate = 0.0F;

    public FrameRateStat() {
    }

    public FrameRateStat(int intervalSecond) {
        if (intervalSecond > 0) {
            this.mIntervalSecond = intervalSecond;
        }

    }

    public float getFrameRate() {
        return this.mRealFrameRate;
    }

    public void calFrameRate() {
        if (this.mFrameCount == 0) {
            this.mLastTime = System.currentTimeMillis();
        }

        ++this.mFrameCount;
        if (System.currentTimeMillis() - this.mLastTime >= (long) (this.mIntervalSecond * 1000)) {
            this.mRealFrameRate = (float) this.mFrameCount / (float) this.mIntervalSecond;
            this.mFrameCount = 0;
        }

    }
}
