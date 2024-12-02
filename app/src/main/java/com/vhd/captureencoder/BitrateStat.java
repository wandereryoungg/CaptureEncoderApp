package com.vhd.captureencoder;

public class BitrateStat {

    public static final int FRAMERATE_STAT_INTERVAL_SECOND_DEFAULT = 2;

    private int mIntervalSecond = FRAMERATE_STAT_INTERVAL_SECOND_DEFAULT;
    private long mLastTime = 0;
    private long mInByte = 0;
    private long mRealBitrate = 0;
    private long mMaxBitrate = 0;
    private long mMinBitrate = 0;

    public BitrateStat() {
    }

    public BitrateStat(int intervalSecond) {
        if (intervalSecond > 0) {
            mIntervalSecond = intervalSecond;
        }
    }

    public long getBitrate() {
        return mRealBitrate;
    }

    public long getMaxBitrate() {
        return mMaxBitrate;
    }

    public long getMinBitrate() {
        return mMinBitrate;
    }

    public void calBitrate(int inByte) {
        if (mInByte == 0) {
            mLastTime = System.currentTimeMillis();
        }

        if (mMinBitrate == 0) {
            mMinBitrate = mRealBitrate;
        }

        mInByte += inByte;
        if ((System.currentTimeMillis() - mLastTime) >= mIntervalSecond * 1000) {
            mRealBitrate = mInByte * 8 / 1000 / ((System.currentTimeMillis() - mLastTime) / 1000);
            mLastTime = System.currentTimeMillis();
            if (mRealBitrate > mMaxBitrate) {
                mMaxBitrate = mRealBitrate;
            }
            if (mRealBitrate < mMinBitrate) {
                mMinBitrate = mRealBitrate;
            }
            mInByte = 0;
        }
    }
}
