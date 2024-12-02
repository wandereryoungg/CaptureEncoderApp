package com.vhd.captureencoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Player implements SurfaceHolder.Callback {

    private String TAG = getClass().getSimpleName();
    public String LOG_MODULE_TAG = "";
    private final static int DEQUEUE_TIMEOUT = 30000; // us
    private static AtomicInteger PLAYER_NUM = new AtomicInteger(0);

    private final static String InputThreadNamePrefix = "VHDPlayerDecoderInputThread_";
    private final static String OutputThreadNamePrefix = "VHDPlayerDecoderOutputThread_";

    private MediaCodec mMediaCodec;
    private SurfaceView mPlayerView;
    private String mMimeType;
    private String mCodecName;

    private int mWidth;
    private int mHeight;
    private int mColorFormat;
    private int mFrameRate;

    private LinkedBlockingQueue<FrameData> mFrameQueueIn;
    private LinkedBlockingQueue<FrameData> mFrameQueueOut;

    private AtomicBoolean mIsRunning = new AtomicBoolean(false);
    private Thread mDecoderInputThread = null;
    private Thread mDecoderOutputThread = null;

    private FrameRateStat mFrameRateStat = new FrameRateStat();
    private BitrateStat mBitrateStat = new BitrateStat();
    private FrameElapsedTimeStat mFrameElapsedTimeStat = new FrameElapsedTimeStat();

    private boolean mIsRender;


    public Player(SurfaceView surfaceView,
                  String mimeType,
                  String codeName,
                  int width,
                  int height,
                  int colorFormat,
                  int frameRate,
                  LinkedBlockingQueue<FrameData> frameQueueIn,
                  LinkedBlockingQueue<FrameData> frameQueueOut) {
        LOG_MODULE_TAG = this.getClass().toString();

        mPlayerView = surfaceView;
        mMimeType = mimeType;
        mCodecName = codeName;
        mWidth = width;
        mHeight = height;
        mColorFormat = colorFormat;
        mFrameRate = frameRate;
        mFrameQueueIn = frameQueueIn;
        mFrameQueueOut = frameQueueOut;


        mIsRender = !(mFrameQueueOut != null); // true

        if (mPlayerView != null) {
            mPlayerView.getHolder().addCallback(this);
        }
    }


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    public void stopThread() {
        Log.i(TAG, "stopThread ====> ");

        mIsRunning.set(false);
        try {
            if (mDecoderInputThread != null) {
                mDecoderInputThread.join(500);
            }
            if (mDecoderOutputThread != null) {
                mDecoderOutputThread.join(500);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mDecoderInputThread = null;
        mDecoderOutputThread = null;
        stopMediaCodec();

        Log.i(TAG, "stopThread end ====> ");
    }

    public void startThread() {

        Log.i(TAG, "startThread ====>");
        Log.i(TAG, "mimeType:" + mMimeType +
                " codecName:" + mCodecName +
                " width:" + mWidth +
                " height:" + mHeight +
                " frameRate:" + mFrameRate +
                " colorFormat:" + mColorFormat);
        if (!startMediaCodec()) {
            return;
        }
        int mNum = PLAYER_NUM.incrementAndGet();
        mIsRunning.set(true);
        mDecoderInputThread = new Thread(new Runnable() {
            @Override
            public void run() {
                long presentationTimeUs;
                long numInputFrames = 0;
                long timestamp;
                int size;
                FrameData bufEncoded = null;
                ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
                Log.i(TAG, "start input loop ====>");
                int inputBufferIndex;
                while (mIsRunning.get()) {
                    Log.v(TAG, "mFrameQueueIn poll size:" + mFrameQueueIn.size());
                    if (bufEncoded == null) {
                        bufEncoded = mFrameQueueIn.poll();
                    }
                    if (bufEncoded != null
                            && bufEncoded.mData != null
                            && bufEncoded.mData.length > 0) {
                        timestamp = System.currentTimeMillis();
                        Log.v(TAG, "decode elapsed time from encode,"
                                + " encode timestamp: " + bufEncoded.mTimestamp
                                + " current timestamp: " + timestamp
                                + " delay: " + (timestamp - bufEncoded.mTimestamp));
                        mFrameElapsedTimeStat.startTime();
                        try {
                            inputBufferIndex = mMediaCodec.dequeueInputBuffer(DEQUEUE_TIMEOUT);
                        } catch (Exception e) {
                            e.printStackTrace();
                            mIsRunning.set(false);
                            break;
                        }
                        if (inputBufferIndex >= 0) {
                            Log.v(TAG, "dequeueInputBuffer inputBufferIndex:" + inputBufferIndex);
                            size = bufEncoded.len == -1 ? bufEncoded.mData.length : bufEncoded.len;

                            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                            inputBuffer.clear();

                            if (size > inputBuffer.limit()) {
                                Log.e(TAG, "dequeueInputBuffer inputBufferIndex:" + inputBufferIndex
                                        + " size is too large:" + size);
                                size = inputBuffer.limit();
                            }

                            inputBuffer.put(bufEncoded.mData, 0, size);
                            mBitrateStat.calBitrate(size);
                            if (mFrameRateStat.mFrameCount % mFrameRate == 0) {
                                HashMap<String, Integer> param = new HashMap<>();
                                param.put("bitrate", (int) mBitrateStat.getBitrate());
                                Log.e(TAG, "bitrate: " + (int) mBitrateStat.getBitrate());
                            }
                            Log.v(TAG, "dequeueInputBuffer inputBufferIndex:" + inputBufferIndex
                                    + " size:" + (bufEncoded.len == -1 ? bufEncoded.mData.length : bufEncoded.len));

                            presentationTimeUs = numInputFrames * 1000000 / mFrameRate;
                            numInputFrames++;
                            Log.v(TAG, "numInputFrames:" + numInputFrames + " presentationTimeUs: " + presentationTimeUs);
                            mMediaCodec.queueInputBuffer(inputBufferIndex,
                                    0,
                                    bufEncoded.len == -1 ? bufEncoded.mData.length : bufEncoded.len,
                                    presentationTimeUs,
                                    0);
                            bufEncoded = null;
                        } else {
                            Log.w(TAG, "dequeueInputBuffer inputBufferIndex error:" + inputBufferIndex);
                        }
                    } else {
                        Log.d(TAG, " poll frame fail");
                        try {
                            Thread.sleep(6);
                        } catch (InterruptedException ignored) {
                        }
                        bufEncoded = null;
                    }
                }
                Log.i(TAG, "end input loop ====>");
            }
        });

        mDecoderInputThread.setName(InputThreadNamePrefix + mNum);
        mDecoderInputThread.start();

        mDecoderOutputThread = new Thread() {
            @Override
            public void run() {
                long presentationTimeUs = 0;
                long numOutputFrames = 0;
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                ByteBuffer[] outputBuffers = mMediaCodec.getOutputBuffers();
                int outputBufferIndex;
                Log.i(TAG, "start output loop ====>");

                while (mIsRunning.get()) {
                    try {
                        outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, DEQUEUE_TIMEOUT);
                    } catch (Exception e) {
                        e.printStackTrace();
                        mIsRunning.set(false);
                        break;
                    }
                    if (outputBufferIndex >= 0) {
                        Log.v(TAG, "dequeueOutputBuffer outputBufferIndex:" + outputBufferIndex);
                        mFrameElapsedTimeStat.endTime();
                        mFrameRateStat.calFrameRate();

                        if (mFrameRateStat.mFrameCount % mFrameRate == 0) {
                            HashMap<String, Integer> param = new HashMap<>();
                            param.put("fps", (int) mFrameRateStat.mRealFrameRate);
                        }

                        Log.v(TAG, "frame count:" + mFrameRateStat.mFrameCount
                                + " interval:" + mFrameRateStat.mIntervalSecond
                                + " fps:" + mFrameRateStat.getFrameRate());
                        Log.v(TAG, "decode start time:" + mFrameElapsedTimeStat.mStartTime
                                + " end time:" + mFrameElapsedTimeStat.mEndTime
                                + " current elapsed time:" + (mFrameElapsedTimeStat.mEndTime - mFrameElapsedTimeStat.mStartTime)
                                + " average elapsed time:" + mFrameElapsedTimeStat.mElapsedTimeAverage
                                + " max elapsed time:" + mFrameElapsedTimeStat.mElapsedTimeMax
                                + " min elapsed time:" + mFrameElapsedTimeStat.mElapsedTimeMin);


                        if (mFrameQueueOut != null) {
                            ByteBuffer byteBuffer = outputBuffers[outputBufferIndex];
                            byteBuffer.position(bufferInfo.offset);
                            byteBuffer.limit(bufferInfo.offset + bufferInfo.size);
                            byte[] data = new byte[bufferInfo.size];
                            outputBuffers[outputBufferIndex].get(data, 0, bufferInfo.size);

                            FrameData frameData = new FrameData();
                            frameData.mTimestamp = System.currentTimeMillis();
                            frameData.mData = data;
                            if (!mFrameQueueOut.offer(frameData)) {
                                Log.w(TAG, "mFrameQueueOut offer fail, size:" + mFrameQueueOut.size());
                            } else {
                                Log.v(TAG, "mFrameQueueOut offer size:" + mFrameQueueOut.size());
                            }

                        }

                        ++numOutputFrames;
                        Log.v(TAG, " numOutputFrames:" + numOutputFrames);
                        presentationTimeUs = numOutputFrames * 1000000 / mFrameRate;
                        mMediaCodec.releaseOutputBuffer(outputBufferIndex, mIsRender);
//                        mMediaCodec.releaseOutputBuffer(outputBufferIndex, presentationTimeUs);
                    } else if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        Log.v(TAG, "no output from decoder available");
                    } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        // The storage associated with the direct ByteBuffer may already be unmapped,
                        // so attempting to access data through the old output buffer array could
                        // lead to a native crash.
                        Log.w(TAG, "decoder output buffers changed");
                        outputBuffers = mMediaCodec.getOutputBuffers();
                    } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        // this happens before the first frame is returned
                        MediaFormat outputFormat = mMediaCodec.getOutputFormat();
                        mWidth = outputFormat.getInteger(MediaFormat.KEY_WIDTH);
                        mHeight = outputFormat.getInteger(MediaFormat.KEY_HEIGHT);
                        Log.e(TAG, "decoder output format changed: " +
                                outputFormat + " width:" + mWidth + " height:" + mHeight);
                        HashMap<String, Integer> param = new HashMap<>();
                        param.put("width", mWidth);
                        param.put("height", mHeight);

                    } else if (outputBufferIndex < 0) {
                        Log.w(TAG, "unexpected result from decoder.dequeueOutputBuffer: " + outputBufferIndex);
                    }
                }

                Log.i(TAG, "end output loop ====>");
            }
        };
        mDecoderOutputThread.setName(OutputThreadNamePrefix + mNum);
        mDecoderOutputThread.start();

        Log.i(TAG, "startThread end ====>");
    }


    private boolean startMediaCodec() {

        if (mIsRunning.get()) {
            return false;
        }

        MediaFormat mediaFormat = MediaFormat.createVideoFormat(mMimeType, mWidth, mHeight);
        //mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, mColorFormat);
        //byte[] header_HD_sps = {0, 0, 0, 1, 103, 100, 64, 41, -84, 44, -88, 5, 0, 91, -112};
        //byte[] header_pps = {0, 0, 0, 1, 104, -18, 56, -128};
        //mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(header_HD_sps));
        //mediaFormat.setByteBuffer("csd-1", ByteBuffer.wrap(header_pps));
        //mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mBitrate);
        //mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mFrameRate);

        try {
            if (mCodecName != null && !mCodecName.isEmpty()) {
                Log.e(TAG, "mCodecName: " + mCodecName);
                mMediaCodec = MediaCodec.createByCodecName(mCodecName);
            } else {
                mMediaCodec = MediaCodec.createDecoderByType(mMimeType);
            }
            mMediaCodec.configure(mediaFormat, mIsRender ? mPlayerView.getHolder().getSurface() : null, null, 0);
            mMediaCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "create MediaCodec fail");
            return false;
        }

        return true;
    }

    private void stopMediaCodec() {
        try {
            if (mMediaCodec != null) {
                mMediaCodec.stop();
                mMediaCodec.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        mMediaCodec = null;
    }

}
