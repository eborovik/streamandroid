package com.eborovik.streamer;

import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.eborovik.streamer.models.LiveVideoModel;
import com.eborovik.streamer.network.NetworkRequest;
import com.eborovik.streamer.network.SignalRClient;
import com.google.android.material.navigation.NavigationView;
import com.pedro.encoder.input.video.CameraHelper;
import com.pedro.encoder.input.video.CameraOpenException;
import com.pedro.rtplibrary.rtmp.RtmpCamera1;

import net.ossrs.rtmp.ConnectCheckerRtmp;

import java.util.ArrayList;
import java.util.List;

public class RtmpActivity extends AppCompatActivity
        implements Button.OnClickListener, ConnectCheckerRtmp, SurfaceHolder.Callback,
        View.OnTouchListener {

    private Integer[] orientations = new Integer[] { 0, 90, 180, 270 };

    private LiveVideoModel videoModel;
    private RtmpCamera1 rtmpCamera1;
    private Button bStartStop, bRecord;
    private EditText etUrl;
    private String currentDateAndTime = "";

    //options menu
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private RadioGroup rgChannel;
    private Spinner spResolution;
    private CheckBox cbEchoCanceler, cbNoiseSuppressor;
    private EditText etVideoBitrate, etFps, etAudioBitrate, etSampleRate;
    private String lastVideoBitrate;
    private TextView tvBitrate;

    private AuthHelper mAuthHelper;
    private SignalRClient src;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_rtmp);

        mAuthHelper = AuthHelper.getInstance(this);
        if (mAuthHelper.isLoggedIn()) {
            setupView();
            src = new SignalRClient("http://10.0.2.2:7000/streamhub");
            src.setCallback(startRecordingCallback);
            src.setStopStreamCallback(stopStreamCallback);
        } else {
            finish();
        }
    }

    private void setupView(){
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        SurfaceView surfaceView = findViewById(R.id.surfaceView);
        surfaceView.getHolder().addCallback(this);
        surfaceView.setOnTouchListener(this);
        rtmpCamera1 = new RtmpCamera1(surfaceView, this);
        prepareOptionsMenuViews();
        tvBitrate = findViewById(R.id.tv_bitrate);

        bStartStop = findViewById(R.id.b_start_stop);
        bStartStop.setOnClickListener(this);

        Button switchCamera = findViewById(R.id.switch_camera);
        switchCamera.setOnClickListener(this);
    }

    private void prepareOptionsMenuViews() {
        drawerLayout = findViewById(R.id.activity_custom);
        navigationView = findViewById(R.id.nv_rtp);

        navigationView.inflateMenu(R.menu.options_rtmp);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.rtmp_streamer,
                R.string.rtmp_streamer) {

            public void onDrawerOpened(View drawerView) {
                actionBarDrawerToggle.syncState();
                lastVideoBitrate = etVideoBitrate.getText().toString();
            }

            public void onDrawerClosed(View view) {
                actionBarDrawerToggle.syncState();
                if (lastVideoBitrate != null && !lastVideoBitrate.equals(
                        etVideoBitrate.getText().toString()) && rtmpCamera1.isStreaming()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        int bitrate = Integer.parseInt(etVideoBitrate.getText().toString()) * 1024;
                        rtmpCamera1.setVideoBitrateOnFly(bitrate);
                        Toast.makeText(RtmpActivity.this, "New bitrate: " + bitrate, Toast.LENGTH_SHORT).
                                show();
                    } else {
                        Toast.makeText(RtmpActivity.this, "Bitrate on fly ignored, Required min API 19",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        };
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        //checkboxs
        cbEchoCanceler =
                (CheckBox) navigationView.getMenu().findItem(R.id.cb_echo_canceler).getActionView();
        cbNoiseSuppressor =
                (CheckBox) navigationView.getMenu().findItem(R.id.cb_noise_suppressor).getActionView();

        rgChannel = (RadioGroup) navigationView.getMenu().findItem(R.id.channel).getActionView();

        //spinners
        spResolution = (Spinner) navigationView.getMenu().findItem(R.id.sp_resolution).getActionView();

        ArrayAdapter<Integer> orientationAdapter =
                new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item);
        orientationAdapter.addAll(orientations);

        ArrayAdapter<String> resolutionAdapter =
                new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item);
        List<String> list = new ArrayList<>();
        for (Camera.Size size : rtmpCamera1.getResolutionsBack()) {
            list.add(size.width + "X" + size.height);
        }
        resolutionAdapter.addAll(list);
        spResolution.setAdapter(resolutionAdapter);
        //edittexts
        etVideoBitrate =
                (EditText) navigationView.getMenu().findItem(R.id.et_video_bitrate).getActionView();
        etFps = (EditText) navigationView.getMenu().findItem(R.id.et_fps).getActionView();
        etAudioBitrate =
                (EditText) navigationView.getMenu().findItem(R.id.et_audio_bitrate).getActionView();
        etSampleRate = (EditText) navigationView.getMenu().findItem(R.id.et_samplerate).getActionView();
        etVideoBitrate.setText("2500");
        etFps.setText("30");
        etAudioBitrate.setText("128");
        etSampleRate.setText("44100");

    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        actionBarDrawerToggle.syncState();
    }

    public static Intent getCallingIntent(Context context) {
        return new Intent(context, RtmpActivity.class);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.openDrawer(GravityCompat.START);
                } else {
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
                return true;
            case R.id.microphone:
                if (!rtmpCamera1.isAudioMuted()) {
                    item.setIcon(getResources().getDrawable(R.drawable.icon_microphone_off));
                    rtmpCamera1.disableAudio();
                } else {
                    item.setIcon(getResources().getDrawable(R.drawable.icon_microphone));
                    rtmpCamera1.enableAudio();
                }
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.b_start_stop:
                NetworkRequest request = new NetworkRequest();
                if (!rtmpCamera1.isStreaming()) {

                    request.startStream(mAuthHelper.getIdToken(), mAuthHelper.getStreamId(), startStreamCallback);

                }
                else {
                    request.stopStream(mAuthHelper.getIdToken(), mAuthHelper.getStreamId(), null);
                    stopStream();
                }
                break;

            case R.id.switch_camera:
                try {
                    rtmpCamera1.switchCamera();
                } catch (final CameraOpenException e) {
                    Toast.makeText(RtmpActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

    private void startStream(String url){
        //bStartStop.setText(getResources().getString(R.string.stop_button));

        if (rtmpCamera1.isRecording() || prepareEncoders()) {

            rtmpCamera1.startStream(url);
        } else {
            //Toast.makeText(this, "Error preparing stream, this device doesn't support it",
                    //Toast.LENGTH_SHORT).show();
           // bStartStop.setText(getResources().getString(R.string.start_button));
        }
    }

    private void stopStream(){
        //bStartStop.setText(getResources().getString(R.string.start_button));
        rtmpCamera1.stopStream();
    }

    private boolean prepareEncoders() {
        Camera.Size resolution =
                rtmpCamera1.getResolutionsBack().get(spResolution.getSelectedItemPosition());
        int width = resolution.width;
        int height = resolution.height;
        return rtmpCamera1.prepareVideo(width, height, Integer.parseInt(etFps.getText().toString()),
                Integer.parseInt(etVideoBitrate.getText().toString()) * 1024,
                CameraHelper.getCameraOrientation(this)) && rtmpCamera1.prepareAudio(
                Integer.parseInt(etAudioBitrate.getText().toString()) * 1024,
                Integer.parseInt(etSampleRate.getText().toString()),
                rgChannel.getCheckedRadioButtonId() == R.id.rb_stereo, cbEchoCanceler.isChecked(),
                cbNoiseSuppressor.isChecked());
    }

    private SignalRClient.Callback<String> startRecordingCallback = new SignalRClient.Callback<String> () {
        @Override
        public void execute(String url) {
            stopStream();
            startStream(url);
        }
    };

    private SignalRClient.StopStreamCallback stopStreamCallback = new SignalRClient.StopStreamCallback () {
        @Override
        public void execute() {
            stopStream();
        }
    };

    @Override
    public void onConnectionSuccessRtmp() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(RtmpActivity.this, "Connection success", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onConnectionFailedRtmp(final String reason) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(RtmpActivity.this, "Connection failed. " + reason, Toast.LENGTH_SHORT)
                        .show();
                rtmpCamera1.stopStream();
                bStartStop.setText(getResources().getString(R.string.start_button));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
                        && rtmpCamera1.isRecording()) {
                    rtmpCamera1.stopRecord();
                    bRecord.setText(R.string.start_record);
                }
            }
        });
    }

    @Override
    public void onNewBitrateRtmp(final long bitrate) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvBitrate.setText(bitrate + " bps");
            }
        });
    }

    @Override
    public void onDisconnectRtmp() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(RtmpActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
                        && rtmpCamera1.isRecording()) {
                    rtmpCamera1.stopRecord();
                    bRecord.setText(R.string.start_record);
                }
            }
        });
    }

    @Override
    public void onAuthErrorRtmp() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(RtmpActivity.this, "Auth error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onAuthSuccessRtmp() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(RtmpActivity.this, "Auth success", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        drawerLayout.openDrawer(GravityCompat.START);
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        rtmpCamera1.startPreview();
        // optionally:
        //rtmpCamera1.startPreview(CameraHelper.Facing.BACK);
        //or
        //rtmpCamera1.startPreview(CameraHelper.Facing.FRONT);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && rtmpCamera1.isRecording()) {
            rtmpCamera1.stopRecord();
            bRecord.setText(R.string.start_record);
            currentDateAndTime = "";
        }
        if (rtmpCamera1.isStreaming()) {
            rtmpCamera1.stopStream();
            bStartStop.setText(getResources().getString(R.string.start_button));
        }
        rtmpCamera1.stopPreview();
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        if (motionEvent.getPointerCount() > 1) {
            if (action == MotionEvent.ACTION_MOVE) {
                rtmpCamera1.setZoom(motionEvent);
            }
        } else {
            if (action == MotionEvent.ACTION_UP) {
                // todo place to add autofocus functional.
            }
        }
        return true;
    }

    private NetworkRequest.Callback<LiveVideoModel> startStreamCallback = new NetworkRequest.Callback<LiveVideoModel>() {
        @Override
        public void onResponse(@NonNull LiveVideoModel response) {
            videoModel = response;

            if(mAuthHelper.getStreamId() == null){
                mAuthHelper.setStreamId(videoModel.getStreamId());
            }

            startStream(videoModel.getUrl());
            src.start(mAuthHelper.getStreamId());
            etUrl = findViewById(R.id.et_rtp_url);
            etUrl.setHint(videoModel.getUrl());
        }

        @Override
        public void onError(String error) {
            Toast.makeText(RtmpActivity.this, error, Toast.LENGTH_SHORT).show();
        }

        @Override
        public Class<LiveVideoModel> type() {
            return LiveVideoModel.class;
        }
    };
}
