package com.simpleflashlight;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import com.simpleflashlight.utility.CommonUtil;

public class MainActivity extends AppCompatActivity implements SensorListener {

    private Camera mCameraFlash = null;                 // for lower than (M - 6.0)
    private CameraManager mCameraManager = null;        // for (M-6.0) and above
    private SensorManager mSensorManager = null;
    private MediaPlayer mMediaPlayerOnSound = null;
    private MediaPlayer mMediaPlayerOffSound = null;

    private static int SHAKE_THRESHOLD = 1500;
    private boolean mIsFlashLight = false;
    private boolean mIsFlashOn = false;
    private int mStoreDeviceBright = 0;
    private int mDeviceBrightMode = 0;
    private boolean mIsOpenSettings = false;
    private boolean mSoundControl = true;

    private Button mTurnOnOffFlashLight = null;
    private View mFullScreenWhiteView = null;
    private Button mFullScreenBrightButton  = null;
    private ImageView mSoundControlView = null;

    private static final int PERMISSIONS_REQUEST_CAMERA_WRITE_SETTINGS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);

            mTurnOnOffFlashLight = (Button) findViewById(R.id.turn_on_off_flash_light);
            mFullScreenWhiteView = (View) findViewById(R.id.full_screen_white_view);
            mFullScreenBrightButton = (Button) findViewById(R.id.full_screen_bright_button);
            mSoundControlView = (ImageView) findViewById(R.id.sound_control_image_view);

            // Ask Permissions from user
//            multiplePermissions();

            // declaring torch sound
//            controlSound(true);

            // checking if phone has flash light or not
            mIsFlashLight = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
            if(!mIsFlashLight) {
                return;
            }
            // getting sensor
            mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

            //.. setting some full screen constrains and screen always on

            WindowManager.LayoutParams attrs = getWindow().getAttributes();
            attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
            attrs.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
            getWindow().setAttributes(attrs);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);

            if(mTurnOnOffFlashLight != null) {
                mTurnOnOffFlashLight.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(mIsFlashOn) {
                            turnOffFlashLight();
                        } else {
                            turnOnFlashLight();
                        }
                    }
                });
            }

            //--- setting dispatch

            if(mFullScreenBrightButton != null) {
                mFullScreenBrightButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        try {
                            turnOffFlashLight();
                            unRegisterSensor();
                            controlScreenBrightView(true, false);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

            if(mFullScreenWhiteView != null) {
                mFullScreenWhiteView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        controlScreenBrightView(false, false);
                        registerSensor(false);
                    }
                });
            }

            if(mSoundControlView != null) {
                mSoundControlView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(mSoundControl) {
                            mSoundControlView.setImageDrawable(getResources().getDrawable(R.drawable.sound_off));
                            mFullScreenBrightButton.setSoundEffectsEnabled(false);
                            mTurnOnOffFlashLight.setSoundEffectsEnabled(false);
                            controlSound(false);
                            mSoundControl = false;
                        } else {
                            mSoundControlView.setImageDrawable(getResources().getDrawable(R.drawable.sound_on));
                            mFullScreenBrightButton.setSoundEffectsEnabled(true);
                            mTurnOnOffFlashLight.setSoundEffectsEnabled(true);
                            controlSound(true);
                            mSoundControl = true;
                        }
                    }
                });
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void registerSensor(boolean isFlashOn) {
        if(mSensorManager == null) {
            return;
        }
        try {
            if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (requestCameraPermission()) {
                    mSensorManager.registerListener(this, SensorManager.SENSOR_ACCELEROMETER, SensorManager.SENSOR_DELAY_GAME);
                    if(isFlashOn)
                        turnOnFlashLight();
                }
            } else {
                mSensorManager.registerListener(this, SensorManager.SENSOR_ACCELEROMETER, SensorManager.SENSOR_DELAY_GAME);
                if(isFlashOn)
                    turnOnFlashLight();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void unRegisterSensor() {
        if(mSensorManager == null) {
            return;
        }
        try {
            mSensorManager.unregisterListener(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void controlScreenBrightView(boolean showBright, boolean onPauseState) {
        if(mTurnOnOffFlashLight == null || mFullScreenBrightButton == null || mFullScreenWhiteView == null) {
            return;
        }
        try {
            mIsOpenSettings = false;
            if(showBright) {
                if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (Settings.System.canWrite(this)) {
                        mStoreDeviceBright = Settings.System.getInt(this.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 80);
                        mDeviceBrightMode = Settings.System.getInt(this.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, 1);
                        Settings.System.putInt(this.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                        Settings.System.putInt(this.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 255);
                    } else {
                        openSettingIntentGetPermission();
                        mIsOpenSettings = true;
                        return;
                    }
                } else {
                    mDeviceBrightMode = Settings.System.getInt(this.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, 1);
                    mStoreDeviceBright = Settings.System.getInt(this.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 80);
                    Settings.System.putInt(this.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                    Settings.System.putInt(this.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 255);
                }

                mFullScreenWhiteView.setVisibility(View.VISIBLE);
                mFullScreenBrightButton.setVisibility(View.GONE);
                mTurnOnOffFlashLight.setVisibility(View.GONE);
                mSoundControlView.setVisibility(View.GONE);
            } else {
                if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (Settings.System.canWrite(this)) {
                        Settings.System.putInt(this.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, mDeviceBrightMode);
                        Settings.System.putInt(this.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, mStoreDeviceBright);
                    }
                } else {
                    Settings.System.putInt(this.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, mDeviceBrightMode);
                    Settings.System.putInt(this.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, mStoreDeviceBright);
                }
                if(!onPauseState) {
                    mFullScreenBrightButton.setVisibility(View.VISIBLE);
                    mTurnOnOffFlashLight.setVisibility(View.VISIBLE);
                    mFullScreenWhiteView.setVisibility(View.GONE);
                    mSoundControlView.setVisibility(View.VISIBLE);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void openSettingIntentGetPermission() {
        try {
            final Intent intent = new Intent();
            intent.setAction(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setData(Uri.parse("package:" + getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void turnOnFlashLight() {
//        if(mMediaPlayerOnSound == null) {
//            return;
//        }
        try {
            if(mIsFlashLight) {
                if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    mCameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
                    String[] list = mCameraManager.getCameraIdList();
                    mCameraManager.setTorchMode(list[0], true);
                } else {
                    mCameraFlash = Camera.open(0);
                    Camera.Parameters parameters = mCameraFlash.getParameters();
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    mCameraFlash.setParameters(parameters);
                    mCameraFlash.startPreview();
                }
                if(mSoundControl) {
                    mMediaPlayerOnSound.start();
                }
                mTurnOnOffFlashLight.setBackground( getResources().getDrawable(R.mipmap.light_on));
                mIsFlashOn = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void turnOffFlashLight() {
        if(mTurnOnOffFlashLight == null) {
            return;
        }
        try {
            if(mIsFlashLight) {
                if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if(mCameraManager == null) {
                        return;
                    }
                    String[] list = mCameraManager.getCameraIdList();
                    mCameraManager.setTorchMode(list[0], false);
                } else {
                    if(mCameraFlash == null) {
                        return;
                    }
                    mCameraFlash.stopPreview();
                    mCameraFlash.release();
                }
                if(mSoundControl) {
                    mMediaPlayerOffSound.start();
                }
                mTurnOnOffFlashLight.setBackground( getResources().getDrawable(R.mipmap.light_off));
                mIsFlashOn = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void controlSound(boolean isOn) {
        try {
            if(isOn) {
                mMediaPlayerOffSound = MediaPlayer.create(this, R.raw.light_off_sound);
                mMediaPlayerOnSound = MediaPlayer.create(this, R.raw.light_on_sound);
            } else {
                if(mMediaPlayerOnSound != null && mMediaPlayerOffSound != null) {
                    mMediaPlayerOffSound.stop();
                    mMediaPlayerOnSound.stop();
                    mMediaPlayerOffSound = null;
                    mMediaPlayerOnSound = null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        turnOffFlashLight();
        if(mStoreDeviceBright != 0) {
            controlScreenBrightView(false, true);
        }
        controlSound(false);
        unRegisterSensor();
    }

    @Override
    public void onResume() {
        super.onResume();
        controlSound(true);
        int visible = mFullScreenWhiteView.getVisibility();
        if(visible != 0) {
            if(mIsOpenSettings) {
                registerSensor(false);
            } else {
                registerSensor(true);
            }
        } else {    // it never calls because on pause we invisible screen
            if(mStoreDeviceBright != 0) {
                controlScreenBrightView(true, false);
            }
        }
    }

    @Override
    public void onBackPressed() {
        try {
            int visible = mFullScreenWhiteView.getVisibility();
            if(visible == 0) {
                controlScreenBrightView(false, false);
                registerSensor(false);
                return;
            }
            super.onBackPressed();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        if (event.getKeyCode() == KeyEvent.KEYCODE_POWER) {
//            mDontTurnOffFlash = true;
//        }
//        return super.onKeyDown(keyCode, event);
//    }
//
//    @Override
//    public boolean dispatchKeyEvent(KeyEvent event) {
//        if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
//            if (event.getAction() == KeyEvent.ACTION_UP){
//
//                return true;
//            }}
//        return super.dispatchKeyEvent(event);
//    };


    private boolean requestCameraPermission() {
        try {
            int permissionCameraCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA);
//            int permissionWriteCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_SETTINGS);
            if (permissionCameraCheck == PackageManager.PERMISSION_DENIED) {
                CommonUtil.showDialog(this, getString(R.string.dialog_title_information), getString(R.string.dialog_flash_light_permission_message), android.R.drawable.ic_dialog_alert, getString(R.string.dialog_button_ok), new CommonUtil.AlertDialogOnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, PERMISSIONS_REQUEST_CAMERA_WRITE_SETTINGS);
                    }
                });
                return false;
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        try {
            if (requestCode == PERMISSIONS_REQUEST_CAMERA_WRITE_SETTINGS) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    registerSensor(true);
                } else  {
                    finish();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    private void multiplePermissions() {
//        int PERMISSION_ALL = 1;
//        String[] PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.WRITE_SETTINGS};
//
//        if(!hasPermissions(PERMISSIONS)){
//            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
//        } else {
//            registerSensor();
//        }
//    }
//
//    public boolean hasPermissions(String... permissions) {
//        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissions != null) {
//            for (String permission : permissions) {
//                if (ActivityCompat.checkSelfPermission(MainActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {
//                    return false;
//                }
//            }
//        }
//        return true;
//    }

    private long mLastUpdate = 0;
    private float mLastX = 0;
    private float mLastY = 0;
    private float mLastZ = 0;
    private long prevTime = 0;

    @Override
    public void onSensorChanged(int sensor, float[] values) {
        try {
            if (sensor == SensorManager.SENSOR_ACCELEROMETER) {
                long curTime = System.currentTimeMillis();
                // only allow one update every 100ms.
                if ((curTime - mLastUpdate) > 100) {
//                    Log.d("sensor", "INSIDE SENSOR");
                    long diffTime = (curTime - mLastUpdate);
                    mLastUpdate = curTime;

                    float x = values[SensorManager.DATA_X];
                    float y = values[SensorManager.DATA_Y];
                    float z = values[SensorManager.DATA_Z];

                    float speed = Math.abs(x+y+z - mLastX - mLastY - mLastZ) / diffTime * 10000;
//                    Log.d("sensor", "Normal speed: " + speed);
                    if (speed > SHAKE_THRESHOLD) {
                        long time = System.currentTimeMillis();
                        if(time > (prevTime + 1000)) {
                            prevTime = time;
                            if (mIsFlashOn) {
                                turnOffFlashLight();
                            } else {
                                turnOnFlashLight();
                            }
                        }
                        Log.d("sensor", "shake detected w/ speed: " + speed);
                    }
                    mLastX = x;
                    mLastY = y;
                    mLastZ = z;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAccuracyChanged(int i, int i1) {

    }
}
