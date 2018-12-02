package com.thomas.scanlibrary;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.thomas.view.ConfirmDialog;
import com.thomas.view.ViewfinderView;
import com.thomas.zxing.camera.CameraManager;
import com.thomas.zxing.decoding.CaptureActivityHandler;
import com.thomas.zxing.decoding.InactivityTimer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Vector;


/**
 * Initial the camera
 *
 * @author Ryan.Tang
 */
public class MipcaActivityCapture extends Activity implements Callback {

    private CaptureActivityHandler handler;
    private ViewfinderView viewfinderView;
    private boolean hasSurface;
    private Vector<BarcodeFormat> decodeFormats;
    private String characterSet;
    private InactivityTimer inactivityTimer;
    private MediaPlayer mediaPlayer;
    private boolean playBeep;
    private static final float BEEP_VOLUME = 0.10f;
    private boolean vibrate;
    private Context active = this;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        int id = getResources().getIdentifier("activity_capture"
                , "layout", getPackageName());
//        setContentView(R.layout.activity_capture);
        setContentView(id);
        //ViewUtil.addTopView(getApplicationContext(), this, R.string.scan_card);
        CameraManager.init(getApplication());
        ;

//        viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
        viewfinderView = (ViewfinderView) findViewById(getId("viewfinder_view", "id"));
//        CheckBox flashlight = (CheckBox) findViewById(R.id.scan_flashlight);
        CheckBox flashlight = (CheckBox) findViewById(getId("scan_flashlight", "id"));
        CheckBox scanInput = (CheckBox) findViewById(getId("scan_type_input", "id"));
        scanInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //显示弹窗输入编码
                final ConfirmDialog confirmDialog = new ConfirmDialog(active);
                confirmDialog.show();
                confirmDialog.setClicklistener(new ConfirmDialog.ClickListenerInterface() {
                    @Override
                    public void doConfirm(String editStr) {
                        // TODO Auto-generated method stub
                        confirmDialog.dismiss();
                        System.out.println(editStr);
                        //toUserHome(context);
                        if(editStr!=null&&editStr.equals("")){
                            Toast.makeText(active,"请输入编码",Toast.LENGTH_LONG).show();
                        }else {
                            setResult("3",editStr);
                        }
                    }

                    @Override
                    public void doCancel() {
                        // TODO Auto-generated method stub
                        confirmDialog.dismiss();
                    }
                });
            }

        });
        flashlight.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    openLightOn();
                } else {
                    closeLightOff();
                }
            }
        });
        findViewById(getId("iv_back", "id")).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        //客服中心
        findViewById(getId("iv_service", "id")).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                finish();
//                Intent resultIntent = new Intent();
//                Bundle bundle = new Bundle();
//                bundle.putString("service", "service");
//                resultIntent.putExtras(bundle);
//                setResult(RESULT_OK, resultIntent);
                setResult("2","service");
            }
        });
        hasSurface = false;
        inactivityTimer = new InactivityTimer(this);
    }


    private void setResult(String type,String content){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("type",type);
            jsonObject.put("content",content);
            Intent resultIntent = new Intent();
            Bundle bundle = new Bundle();
            bundle.putString("result", jsonObject.toString());
            resultIntent.putExtras(bundle);
            setResult(RESULT_OK, resultIntent);
            finish();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    private int getId(String idName, String type) {
        return getResources().getIdentifier(idName, type, getPackageName());
    }

    @Override
    protected void onResume() {
        super.onResume();
//        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
        SurfaceView surfaceView = (SurfaceView) findViewById(getId("preview_view", "id"));
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
            initCamera(surfaceHolder);
        } else {
            surfaceHolder.addCallback(this);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
        decodeFormats = null;
        characterSet = null;

        playBeep = true;
        AudioManager audioService = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
            playBeep = false;
        }
        initBeepSound();
        vibrate = true;

    }

    @Override
    protected void onPause() {
        super.onPause();
        closeLightOff();
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        CameraManager.get().closeDriver();
    }

    @Override
    protected void onDestroy() {

        inactivityTimer.shutdown();
        super.onDestroy();
    }


    public void handleDecode(Result result, Bitmap barcode) {
        inactivityTimer.onActivity();
        playBeepSoundAndVibrate();
        String resultString = result.getText();
        if (resultString.equals("")) {
            Toast.makeText(MipcaActivityCapture.this, "Scan failed!", Toast.LENGTH_SHORT).show();
        } else {
            Intent resultIntent = new Intent();
            Bundle bundle = new Bundle();
//            resultString = resultString.replaceAll("[^(a-zA-Z0-9\\u4e00-\\u9fa5)]", "");
//            bundle.putString("result", resultString);

            resultString = resultString.replaceAll("[^(a-zA-Z0-9\\u4e00-\\u9fa5)]", ";");
            String[] splits = resultString.split(";");
            bundle.putString("result", resultString);
            bundle.putString("result", splits[0]);

//            bundle.putParcelable("bitmap", barcode);
            resultIntent.putExtras(bundle);
            setResult("1",splits[0]);
//            this.setResult(RESULT_OK, resultIntent);
        }
        MipcaActivityCapture.this.finish();
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        try {
            CameraManager.get().openDriver(surfaceHolder);
        } catch (IOException ioe) {
            return;
        } catch (RuntimeException e) {
            return;
        }
        if (handler == null) {
            handler = new CaptureActivityHandler(this, decodeFormats,
                    characterSet);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;

    }

    public ViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    public Handler getHandler() {
        return handler;
    }

    public void drawViewfinder() {
        viewfinderView.drawViewfinder();

    }

    private void initBeepSound() {
        if (playBeep && mediaPlayer == null) {
            // The volume on STREAM_SYSTEM is not adjustable, and users found it
            // too loud,
            // so we now play on the music stream.
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setOnCompletionListener(beepListener);

//            AssetFileDescriptor file = getResources().openRawResourceFd(
//                    R.raw.beep);
            AssetFileDescriptor file = getResources().openRawResourceFd(getId("beep", "raw"));
            try {
                mediaPlayer.setDataSource(file.getFileDescriptor(),
                        file.getStartOffset(), file.getLength());
                file.close();
                mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
                mediaPlayer.prepare();
            } catch (IOException e) {
                mediaPlayer = null;
            }
        }
    }

    private static final long VIBRATE_DURATION = 200L;

    private void playBeepSoundAndVibrate() {
        if (playBeep && mediaPlayer != null) {
            mediaPlayer.start();
        }
        if (vibrate) {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            vibrator.vibrate(VIBRATE_DURATION);
        }
    }

    /**
     * When the beep has finished playing, rewind to queue up another one.
     */
    private final OnCompletionListener beepListener = new OnCompletionListener() {
        public void onCompletion(MediaPlayer mediaPlayer) {
            mediaPlayer.seekTo(0);
        }
    };

    private Camera m_Camera = null;
    private boolean isflashlightOpen = false;

    private void openLightOn() {
        if (isflashlightOpen) return;
        m_Camera = CameraManager.get().getCamera();

        Camera.Parameters parameter = m_Camera.getParameters();

        parameter.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);

        m_Camera.setParameters(parameter);
        isflashlightOpen = true;
    }

    private void closeLightOff() {
        if (isflashlightOpen && m_Camera != null) {
            Camera.Parameters parameter = m_Camera.getParameters();
            parameter.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            m_Camera.setParameters(parameter);
            isflashlightOpen = false;
        }
    }
}
