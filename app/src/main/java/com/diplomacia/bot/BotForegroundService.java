package com.diplomacia.bot;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

public class BotForegroundService extends Service {
    private static final String CHANNEL_ID = "diplomacia_bot_foreground";
    private static final int NOTIFICATION_ID = 1001;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private PowerManager.WakeLock wakeLock;
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            triggerDueRuns();
            handler.postDelayed(this, 1000L);
        }
    };

    static void start(Context context) {
        Intent intent = new Intent(context, BotForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    static void stopIfNoActiveAccount(Context context) {
        if (!hasActiveAccount(context)) {
            context.stopService(new Intent(context, BotForegroundService.class));
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DiplomaciaBot:Foreground");
            wakeLock.acquire();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, notification());
        handler.removeCallbacks(ticker);
        handler.post(ticker);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(ticker);
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void triggerDueRuns() {
        long now = System.currentTimeMillis();
        boolean hasActive = false;
        for (int i = 0; i < 2; i++) {
            BotConfig config = BotConfig.load(this, i);
            if (!config.isReady()) {
                continue;
            }
            hasActive = true;
            long nextRunAt = BotConfig.prefs(this).getLong(BotWorker.nextRunKey(i), 0L);
            if (nextRunAt <= 0L || nextRunAt <= now) {
                if (!BotWorker.isRunning(i)) {
                    BotWorker.runNow(this, i);
                }
            }
        }
        if (!hasActive) {
            stopSelf();
        }
    }

    private static boolean hasActiveAccount(Context context) {
        for (int i = 0; i < 2; i++) {
            if (BotConfig.load(context, i).isReady()) {
                return true;
            }
        }
        return false;
    }

    private Notification notification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return builder
                .setContentTitle("Diplomacia Bot calisiyor")
                .setContentText("Arka planda aktif hesaplar takip ediliyor.")
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Diplomacia Bot",
                NotificationManager.IMPORTANCE_LOW
        );
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }
}
