package com.project.getlatlong;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

public class NetworkChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        try {
            if(isOnline(context))   {
    //            Log.d("Check", "is ONline.");
                MyService.isInternetActive = true;
                if(gpsEnabled())
                    createNotification("App running in background.");
                else
                    createNotification("Gps Not Enabled.");
            }
            else    {
    //            Log.d("Check", "is offline.");
                MyService.isInternetActive = false;
                if(gpsEnabled())
                    createNotification("Turn on the internet.");
                else
                    createNotification("Gps and Internet are off.");
            }
        }   catch (NullPointerException e)  {
            e.printStackTrace();
        }

    }

    private boolean isOnline(Context context)   {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            return (networkInfo != null && networkInfo.isConnected());
        }   catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean gpsEnabled()    {
        LocationManager lm = (LocationManager) MyService.context.getSystemService(Service.LOCATION_SERVICE);
        boolean isEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        createNotification("");
        return isEnabled;
    }

    private void createNotification(String content)   {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(MyService.context, String.valueOf(MyService.CHANNEL_ID))
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("DRCollectApp")
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        Notification notification = mBuilder.build();
        notification.flags = Notification.FLAG_NO_CLEAR;

        MyService.notificationManager = NotificationManagerCompat.from(MyService.context);
        MyService.notificationManager.notify(MyService.notificationId-1, notification);

    }



}
