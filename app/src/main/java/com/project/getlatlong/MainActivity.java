package com.project.getlatlong;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Calendar;

import static com.rvalerio.fgchecker.Utils.hasUsageStatsPermission;


public class MainActivity extends AppCompatActivity {

    static boolean serviceRunning = false;
    static Context context;

    private TextView  textLat, textLon, textRX, textTX, textCid;
    private Button buttonStopTask;

    static Intent intent, intent1;

    //private BroadcastReceiver mScreenActiveReceiver;
    AlarmManager alarmManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("Check", "Activity created.");
        context = this;

        textLat = findViewById(R.id.lat);
        textLon = findViewById(R.id.lon);
        textRX = findViewById(R.id.ds);
        textTX = findViewById(R.id.us);
        textCid = findViewById(R.id.cid);
        buttonStopTask = findViewById(R.id.stopTask);

    //    PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
    //    if(powerManager.isInteractive()) {
            intent = new Intent(this, MyService.class);
            this.startService(intent);
    //    }
    //    else {

    //        intent1 = new Intent(this, ServiceSleepMonitor.class);
    //        this.startService(intent1);

    /*        Calendar calendar = Calendar.getInstance();
            PendingIntent pIntent = PendingIntent.getService(this,
                    0,intent,
                    PendingIntent.FLAG_CANCEL_CURRENT);

            alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            // Start service every hour
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    120000, pIntent);
*/
            //    }

        serviceRunning = true;
        requestUsageStatsPermission();


        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        double lati = intent.getDoubleExtra(MyService.EXTRA_LATITUDE, 0);
                        double longi = intent.getDoubleExtra(MyService.EXTRA_LONGITUDE, 0);
                        textLat.setText(""+lati);
                        textLon.setText(""+longi);
                        textRX.setText(intent.getDoubleExtra("RX", 0)+" KB/s");
                        textTX.setText(intent.getDoubleExtra("TX", 0)+" KB/s");
                        textCid.setText(""+intent.getIntExtra("Cid",0));
                    }
                }, new IntentFilter(MyService.ACTION_LOCATION_BROADCAST)
        );


        buttonStopTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
    //            Log.d("Check", "Button clicked.");
                onDestroy();
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("Check", "Activity OnDestroy()....");
        serviceRunning = false;

    /*    PendingIntent cancelService = PendingIntent.getBroadcast(this,
                0,
                intent,
                0);
        alarmManager.cancel(cancelService);
*/
            stopService(intent);
    //    stopService(intent1);
    }


    void requestUsageStatsPermission() {
        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                && !hasUsageStatsPermission(this)) {
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    boolean hasUsageStatsPermission(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow("android:get_usage_stats",
                android.os.Process.myUid(), context.getPackageName());
        boolean granted = mode == AppOpsManager.MODE_ALLOWED;
        return granted;
    }

}
