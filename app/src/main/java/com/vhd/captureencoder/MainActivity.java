package com.vhd.captureencoder;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;

import androidx.appcompat.app.AppCompatActivity;

import com.vhd.captureencoder.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private String TAG = getClass().getSimpleName();

    static {
        System.load("/system/lib64/libEncodermpp.so");
        System.load("/system/lib64/CaptureEncoder.so");
    }

    private ActivityMainBinding binding;
    private Map<Integer, CaptureModel> mCaptureModels;
    private int mCurrentCameraId = 0;
    private int mCurrentWidth;
    private int mCurrentHeight;
    private int mCurrentFps;

    private EncoderModel encoderModelOne;
    private EncoderModel encoderModelTwo;
    private EncoderModel encoderModelThree;
    private List<EncoderModel> mEncoderModels = new ArrayList<>();


    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnStart.setOnClickListener(this);
        binding.btnStop.setOnClickListener(this);
        binding.btnGetResolution.setOnClickListener(this);
        binding.btnGetFps.setOnClickListener(this);

        mCaptureModels = new HashMap<>();

        binding.rbCamera0.setOnCheckedChangeListener(this);
        binding.rbCamera1.setOnCheckedChangeListener(this);
        binding.rbResolution4K.setOnCheckedChangeListener(this);
        binding.rbResolution1080P.setOnCheckedChangeListener(this);
        binding.rbResolution720P.setOnCheckedChangeListener(this);
        binding.rbFps30.setOnCheckedChangeListener(this);
        binding.rbFps60.setOnCheckedChangeListener(this);

        binding.rbCamera0.setChecked(true);
        binding.rbResolution4K.setChecked(true);
        binding.rbFps60.setChecked(true);

    }

    private void updateCameraModel() {
        Log.i(TAG, "mCurrentCameraId: " + mCurrentCameraId + " mCurrentWidth: " + mCurrentWidth + " mCurrentHeight: " + mCurrentHeight + " mCurrentFps: " + mCurrentFps);
        CaptureModel captureModel = mCaptureModels.get(mCurrentCameraId);
        if (captureModel == null) {
            captureModel = new CaptureModel(mCurrentCameraId, mCurrentWidth, mCurrentHeight, mCurrentFps);
            mCaptureModels.put(mCurrentCameraId, captureModel);
        } else {
            captureModel.updateParams(binding.surfacePreview.getHolder().getSurface(), mCurrentWidth, mCurrentHeight, mCurrentFps);
        }
    }

    @Override
    public void onClick(View v) {
        CaptureModel captureModel = mCaptureModels.get(mCurrentCameraId);
        switch (v.getId()) {
            case R.id.btn_start:

                encoderModelOne = new EncoderModel(binding.surfaceDecode1, mCurrentWidth, mCurrentHeight, mCurrentFps);
                encoderModelTwo = new EncoderModel(binding.surfaceDecode2, mCurrentWidth, mCurrentHeight, 30);
//                encoderModelThree = new EncoderModel(binding.surfaceDecode3, mCurrentWidth, mCurrentHeight, 30);
                mEncoderModels.add(encoderModelOne);
                mEncoderModels.add(encoderModelTwo);
//                mEncoderModels.add(encoderModelThree);

                if (captureModel != null) {
                    for (EncoderModel encoderModel : mEncoderModels) {
                        captureModel.addEncoder(encoderModel);
                        encoderModel.startPlayer();
                    }
                    captureModel.startCapture();
                }
                break;
            case R.id.btn_stop:
                if (captureModel != null) {
                    captureModel.stopCapture();
                    captureModel.removeAllEncoder();
                }
                for (EncoderModel encoderModel : mEncoderModels) {
                    encoderModel.stopPlayer();
                }
                mEncoderModels.clear();
                break;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        CaptureModel captureModel = mCaptureModels.get(mCurrentCameraId);
        if (captureModel != null) {
            captureModel.stopCapture();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (Integer cameraId : mCaptureModels.keySet()) {
            CaptureModel captureModel = mCaptureModels.get(cameraId);
            if (captureModel != null) {
                captureModel.release();
            }
        }
        mCaptureModels.clear();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Log.e(TAG, "onCheckedChanged isChecked: " + isChecked);
        if (isChecked) {
            switch (buttonView.getId()) {
                case R.id.rbCamera0:
                    mCurrentCameraId = 0;
                    break;
                case R.id.rbCamera1:
                    mCurrentCameraId = 1;
                    break;
                case R.id.rbResolution4K:
                    mCurrentWidth = 3840;
                    mCurrentHeight = 2160;
                    updateCameraModel();
                    break;
                case R.id.rbResolution1080P:
                    mCurrentWidth = 1920;
                    mCurrentHeight = 1080;
                    updateCameraModel();
                    break;
                case R.id.rbResolution720P:
                    mCurrentWidth = 1280;
                    mCurrentHeight = 720;
                    updateCameraModel();
                    break;
                case R.id.rbFps30:
                    mCurrentFps = 30;
                    updateCameraModel();
                    break;
                case R.id.rbFps60:
                    mCurrentFps = 60;
                    updateCameraModel();
                    break;
            }
        }
    }
}