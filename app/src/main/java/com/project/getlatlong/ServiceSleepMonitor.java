package com.project.getlatlong;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

public class ServiceSleepMonitor extends Service {

    private boolean firstTimeServiceStart = true;
    private BroadcastReceiver mScreenActiveReceiver_1;
    PowerManager.WakeLock wakeLock;

    static Intent intent;
    static Context context;

    public ServiceSleepMonitor() {
        Log.d("Check","ServiceSleepMonitor : Service constructor...");
        context = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d("Check","ServiceSleepMonitor : Service onCreate()...");

        PowerManager powerManager = (PowerManager) getSystemService(this.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakelockTag");
        wakeLock.acquire();

        if(firstTimeServiceStart) {
            if (powerManager.isInteractive()) {
                firstTimeServiceStart = false;
                startService();
            }
        }

        // REGISTER RECEIVER THAT HANDLES SCREEN ON LOGIC
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mScreenActiveReceiver_1 = new ScreenReceiver();
        registerReceiver(mScreenActiveReceiver_1, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d("Check","ServiceSleepMonitor : Service onStartCommand()...");
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mScreenActiveReceiver_1);
        wakeLock.release();
        firstTimeServiceStart = true;
        stopService();
        Log.d("Check","ServiceSleepMonitor : Service destroyed...");
    }

    public void onTaskRemoved(Intent rootIntent)    {
        Log.d("Check", "ServiceSleepMonitor : onTaskRemoved called.");
    //    stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    static public void startService()   {
        intent = new Intent(context, MyService.class);
        context.startService(intent);
    }

    static public void stopService()   {
        context.stopService(intent);
    }
}
