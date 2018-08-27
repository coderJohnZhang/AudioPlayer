package com.tcl.audioplayer;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private TextView musicStatus;
    private TextView musicTime;
    private TextView musicTotal;
    private SeekBar seekBar;
    private Button btnPlayOrPause;
    private Button btnStop;
    private Button btnQuit;

    private SimpleDateFormat time = new SimpleDateFormat("mm:ss", Locale.getDefault());
    private boolean tag1 = false;
    private boolean tag2 = false;
    private MusicService musicService;

    //  在Activity中调用 bindService 保持与 Service 的通信
    private void bindServiceConnection() {
        Intent intent = new Intent(MainActivity.this, MusicService.class);

        // 启动服务的地方
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        startService(intent);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    //  回调onServiceConnected 函数，通过IBinder 获取 Service对象，实现Activity与 Service的绑定
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            musicService = ((MusicService.MyBinder) (service)).getService();
            Log.i(TAG, "musicService == null ? " + (musicService == null));
            musicTotal.setText(time.format(musicService.getDuration()));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicService = null;
        }
    };

    //  通过 Handler 更新 UI 上的组件状态
    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            musicTime.setText(time.format(musicService.getCurrentPosition()));
            seekBar.setProgress(musicService.getCurrentPosition());
            seekBar.setMax(musicService.getDuration());
            musicTotal.setText(time.format(musicService.getDuration()));
            handler.postDelayed(runnable, 200);

        }
    };

    private void findViewById() {
        musicTime = (TextView) findViewById(R.id.MusicTime);
        musicTotal = (TextView) findViewById(R.id.MusicTotal);
        seekBar = (SeekBar) findViewById(R.id.MusicSeekBar);
        btnPlayOrPause = (Button) findViewById(R.id.BtnPlayorPause);
        btnStop = (Button) findViewById(R.id.BtnStop);
        btnQuit = (Button) findViewById(R.id.BtnQuit);
        musicStatus = (TextView) findViewById(R.id.MusicStatus);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        verifyStoragePermissions(this);
        setContentView(R.layout.activity_main);
        findViewById();
        bindServiceConnection();
        myListener();

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    musicService.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }

    private void myListener() {
        ImageView imageView = (ImageView) findViewById(R.id.Image);
        final ObjectAnimator animator = ObjectAnimator.ofFloat(imageView, "rotation", 0f, 360.0f);
        animator.setDuration(10000);
        animator.setInterpolator(new LinearInterpolator());
        animator.setRepeatCount(-1);
        btnPlayOrPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (musicService == null) {
                    return;
                }
                seekBar.setProgress(musicService.getCurrentPosition());
                seekBar.setMax(musicService.getDuration());
                //  由tag的变换来控制事件的调用
                if (!musicService.isTag()) {
                    btnPlayOrPause.setText(R.string.str_pause);
                    musicStatus.setText(R.string.str_playing);
                    musicService.playOrPause();
                    musicService.setTag(true);

                    if (!tag1) {
                        animator.start();
                        tag1 = true;
                    } else {
                        animator.resume();
                    }
                } else {
                    btnPlayOrPause.setText(R.string.str_play);
                    musicStatus.setText(R.string.str_paused);
                    musicService.playOrPause();
                    animator.pause();
                    musicService.setTag(false);
                }
                if (!tag2) {
                    handler.post(runnable);
                    tag2 = true;
                }
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                musicStatus.setText(R.string.str_stopped);
                btnPlayOrPause.setText(R.string.str_play);
                musicService.stop();
                animator.pause();
                musicService.setTag(false);
            }
        });

        //  停止服务时，必须解除绑定，写入btnQuit按钮中
        btnQuit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handler.removeCallbacks(runnable);
                unbindService(serviceConnection);
                Intent intent = new Intent(MainActivity.this, MusicService.class);
                stopService(intent);
                try {
                    MainActivity.this.finish();
                } catch (Exception e) {
                    Log.e(TAG, "onClick: e = " + e.toString());
                }
            }
        });

    }

    //  获取并设置返回键的点击事件
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(false);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {"android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"};

    public static void verifyStoragePermissions(Activity activity) {
        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity, "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}



