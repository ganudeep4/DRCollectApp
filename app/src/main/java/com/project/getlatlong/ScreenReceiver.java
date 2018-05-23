package com.project.getlatlong;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ScreenReceiver extends BroadcastReceiver {

    public static boolean screenOff;

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.

        if(intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            Log.d("Check", "Screen is OFF.");
    //        context.startService(new Intent(context, ServiceSleepMonitor.class));
    //        context.stopService(new Intent(context, MyService.class));

    //        ServiceSleepMonitor.stopService();
            screenOff = true;

        }   else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON))    {
            Log.d("Check", "Screen is ON.");
            screenOff = false;
     //       ServiceSleepMonitor.startService();


            context.startService(new Intent(context, MyService.class));
    //        context.stopService(new Intent(context, ServiceSleepMonitor.class));
        }
    }

}


