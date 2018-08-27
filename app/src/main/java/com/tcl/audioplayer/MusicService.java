package com.tcl.audioplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import java.io.File;

public class MusicService extends Service {

    private static final String TAG = MusicService.class.getSimpleName();

    private static final String MUSIC_PATH = Environment.getExternalStorageDirectory().
            getPath() + File.separator + "music.mp3";

    private MediaPlayer mediaPlayer;
    private boolean musicTag = false;

    public MusicService() {
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(MUSIC_PATH);
            mediaPlayer.prepare();
            mediaPlayer.setLooping(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //  通过 Binder 来保持 Activity 和 Service 的通信
    private MyBinder binder = new MyBinder();

    public int getDuration() {
        try {
            if (mediaPlayer != null) {
                return mediaPlayer.getDuration();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getCurrentPosition() {
        try {
            if (mediaPlayer != null) {
                return mediaPlayer.getCurrentPosition();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void seekTo(int progress) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.seekTo(progress);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isTag() {
        return musicTag;
    }

    public void setTag(boolean tag) {
        this.musicTag = tag;
    }

    public class MyBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    public void playOrPause() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        } else {
            mediaPlayer.start();
        }
    }

    public void stop() {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.reset();
                mediaPlayer.setDataSource(MUSIC_PATH);
                mediaPlayer.prepare();
                mediaPlayer.seekTo(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: SD card path = " + MUSIC_PATH);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String channelId = "001";
            String channelName = "myChannel";
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE);
            channel.setLightColor(Color.BLUE);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                Notification notification;

                notification = new Notification.
                        Builder(getApplicationContext(), channelId).setOngoing(true).setSmallIcon(R.mipmap.ic_launcher).setCategory(Notification.CATEGORY_SERVICE).build();

                startForeground(101, notification);
            }
        } else {
            startForeground(101, new Notification());
        }
    }

}

