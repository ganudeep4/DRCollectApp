package com.project.getlatlong;

import android.Manifest;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.TrafficStats;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.rvalerio.fgchecker.AppChecker;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MyService extends Service {

    public static Context context;

    boolean mStopHandler;
    public static Boolean isRunning;    // Check for service if it's running.
    boolean mFirstFile;                         // Current existing file. IF AnalysisData.csv = true elseif AnalysisData1.csv = false
    public static boolean isInternetActive;
    boolean mhandlerRunning;
    boolean receiversRegistered = false;

    double RXOld, TXOld, currentRXDataRate, currentTXDataRate;

    public static final String ACTION_LOCATION_BROADCAST = MyService.class.getName() + "LocationBroadcast",
            EXTRA_LATITUDE = "extra_latitude",
            EXTRA_LONGITUDE = "extra_longitude";

    private static LocationManager mLocationManager = null;
    private static final int LOCATION_DISTANCE = 10;
    private static double currentLat = 0;
    private static double currentLong = 0;
    static LocationListener mLocationListenerGps = new LocationListener();
    static LocationListener mLocationListenerNetwork = new LocationListener();

    private String tag = "Check";

    private String currentForegroundAppName = "";
    private TelephonyManager telephonyManager;
    private int cell_id = 0;

    public static int CHANNEL_ID = 1;
    public static int notificationId = 1;
    public static boolean notificationPresent;
    static NotificationManagerCompat notificationManager;

    String baseDir = "/storage/emulated/0/Download";
    String fileName = "AnalysisData.csv";
    String filePath = baseDir + File.separator + fileName;
    File file = new File(filePath);

    String tempFile = "AnalysisData1.csv";
    String tempFilePath = baseDir + File.separator + tempFile;
    File file1 = new File(tempFilePath);
    final private String upload = " (upload (KB/s))";
    final private String download = " (download (KB/s))";

    ArrayList<String> column_names = new ArrayList<String>();

    private BroadcastReceiver mNetworkReceiver;
    private BroadcastReceiver mScreenActiveReceiver;


   // PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(tag, "MyService : oService created...");

        context = this;

    //    PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
    //    wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakelockTag");
    //    wakeLock.acquire();

        registerReceivers();

        RXOld = TrafficStats.getMobileRxBytes();
        TXOld = TrafficStats.getMobileTxBytes();

        notificationPresent = false;
        mStopHandler = true;
        mhandlerRunning = false;
        isRunning = false;

        initializeLocationManager();

        column_names.add("Time Instance");
        column_names.add("Base Station Id");
        column_names.add("GPS Coordinates (lat;lon)");

        currentForegroundAppName = getForegroundAppName();

        if (file.exists())
            mFirstFile = true;
        else if (file1.exists())
            mFirstFile = false;
        else
            mFirstFile = true;

        createNotification(true, "App is running in the background.");

        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        Log.d(tag, "MyService : oonStartCommand");

        startAll();

        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    public void startAll()  {
    //    Log.d(tag, "Handler now running---"+mStopHandler+"  "+mhandlerRunning);
        if(mStopHandler && !mhandlerRunning) {
            mStopHandler = false;
            mhandlerRunning = true;
            mHandlerTask.run();
     //       Log.d(tag, "Handler now running.---"+mStopHandler);
        }
    }


    Handler mHandler = new Handler();
    Runnable mHandlerTask = new Runnable() {
        @Override
        public void run() {

            if (!isRunning) {
                startListening();
                isRunning = true;
    //            Log.d(tag, "Started Listening to location.");
            }

     //       Log.d(tag, "Handler.----"+mStopHandler);


            double overallRXTraffic = TrafficStats.getMobileRxBytes();
            currentRXDataRate = Double.parseDouble(new DecimalFormat(".##").format((overallRXTraffic - RXOld) / 1024));
            RXOld = overallRXTraffic;

            double overallTXTraffic = TrafficStats.getMobileTxBytes();
            currentTXDataRate = Double.parseDouble(new DecimalFormat(".##").format((overallTXTraffic - TXOld) / 1024));
            TXOld = overallTXTraffic;

            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            List<CellInfo> cellInfoList = telephonyManager.getAllCellInfo();
            cell_id = getCellId(cellInfoList);

            if (!ScreenReceiver.screenOff) {
                if(isInternetActive)    {
            //        Log.d(tag, "Service writing in file.");
                    if (currentForegroundAppName.equals(getForegroundAppName())) {
                        writeOrReadFile(mFirstFile, true, getDateTime(),
                                cell_id,
                                currentLat, currentLong,
                                currentForegroundAppName,
                                currentTXDataRate, currentRXDataRate);
                    } else {
                        currentForegroundAppName = getForegroundAppName();
                        writeOrReadFile(mFirstFile, false, getDateTime(),
                                cell_id,
                                currentLat, currentLong,
                                currentForegroundAppName,
                                currentTXDataRate, currentRXDataRate);
                    }
                }
            }   else    {
                Log.d(tag, "else...");
                mStopHandler = true;
            }


            sendBroadcastMessage();
            mHandler.postDelayed(this, 1000);

            if(mStopHandler)
             {
                Log.d(tag, "Handler Callbacks removed....");
                mHandler.removeCallbacks(this);
                mhandlerRunning = false;
             }
        }
    };



    @Override
    public void onDestroy() {
        super.onDestroy();
    //    Log.d(tag,"MyService : Service destroyed...");

        stopAll();

    //    wakeLock.release();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }


    public void onTaskRemoved(Intent rootIntent)    {
    //    Log.d("Check", "MyService : onTaskRemoved method called.");
        stopSelf();
    }

    public void stopAll()   {
        if (mLocationManager != null)
        {
            mLocationManager.removeUpdates(mLocationListenerGps);
            mLocationManager.removeUpdates(mLocationListenerNetwork);
        }

        isRunning = false;
        mStopHandler = true;

        unregisterReceiver(mScreenActiveReceiver);
        unregisterReceiver(mNetworkReceiver);

        Log.d(tag,"MyService : Service destroyed...");

        notificationPresent = false;
        notificationManager.cancelAll();
        mhandlerRunning = false;
    }


    public void registerReceivers() {
        if(!receiversRegistered) {
            // REGISTER RECEIVER THAT HANDLES NETWROK CHANGES i.e INTERNET IF OFF/ON.
            IntentFilter iFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            iFilter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);
            mNetworkReceiver = new NetworkChangeReceiver();
            registerReceiver(mNetworkReceiver, iFilter);

            // REGISTER RECEIVER THAT HANDLES SCREEN ON AND SCREEN OFF LOGIC
            IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_SCREEN_ON);
            mScreenActiveReceiver = new ScreenReceiver();
            registerReceiver(mScreenActiveReceiver, filter);
        }
    }

    public void initializeLocationManager() {
        if (mLocationManager == null) {
    //        Log.d("Check", "initializeLocationManager");
            mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        }
    }


    public void startListening()   {
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000, LOCATION_DISTANCE, mLocationListenerGps);
            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    1000, 0, mLocationListenerNetwork);
        } catch (java.lang.SecurityException ex) {
    //        Log.i("Check", "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
    //        Log.d("Check", "gps provider does not exist " + ex.getMessage());
        }
        isRunning = true;
    }


    private String getForegroundAppName() {
        AppChecker appChecker = new AppChecker();
        String packageName = appChecker.getForegroundApp(this);
        return packageName;
    }

    private String getDateTime() {
        Date currentTime = Calendar.getInstance().getTime();
        DateFormat dateFormatter = new SimpleDateFormat("MMM dd, HH:mm:ss");
        String s = dateFormatter.format(currentTime);
        return s;
    }

    private int getCellId(List<CellInfo> cellInfoList) {
        if (cellInfoList != null) {
            for (CellInfo info : cellInfoList) {
                if (info.isRegistered()) {
                    if (info instanceof CellInfoGsm) {
                        CellInfoGsm cellInfo = (CellInfoGsm) info;
                        CellIdentityGsm cellIdentity = cellInfo.getCellIdentity();
                        return cellIdentity.getCid();
                    } else if (info instanceof CellInfoWcdma) {
                        CellInfoWcdma cellInfo = (CellInfoWcdma) info;
                        CellIdentityWcdma cellIdentity = cellInfo.getCellIdentity();
                        return cellIdentity.getCid();
                    } else if (info instanceof CellInfoCdma) {
                        CellInfoCdma cellInfo = (CellInfoCdma) info;
                        CellIdentityCdma cellIdentity = cellInfo.getCellIdentity();
                        return cellIdentity.getBasestationId();
                    } else if (info instanceof CellInfoLte) {
                        CellInfoLte cellInfo = (CellInfoLte) info;
                        CellIdentityLte cellIdentity = cellInfo.getCellIdentity();
                        return cellIdentity.getCi();
                    } else {
                        //                       Log.d("Check", "Unknown type of cell signal!" + "\n ClassName: " + info.getClass().getSimpleName() + "\n ToString: " + info.toString());
                        Toast.makeText(this, "Unknown type of cell signal!" + "\n ClassName: " + info.getClass().getSimpleName() + "\n ToString: " + info.toString(),
                                Toast.LENGTH_LONG).show();
                        return 0;
                    }
                }
            }
        }
        return 1;
    }

    private void writeOrReadFile(boolean existingFileIsFirstOne, boolean sameApp, String time, int cellId, Double lat, Double lon, String currentApp, double upload_rate, double download_rate) {
        CSVWriter writer = null;
        int index_1 = -1;
        int index_2 = -1;

        String currentPath;
        File currentFile;
        if (existingFileIsFirstOne) {
            currentFile = file;
            currentPath = filePath;
        } else {
            currentFile = file1;
            currentPath = tempFilePath;
        }

        if (sameApp) {
            try {
                if (currentFile.exists()) {
                    FileWriter mFileWriter = new FileWriter(currentPath, true);
                    writer = new CSVWriter(mFileWriter);

                    CSVReader reader = new CSVReader(new FileReader(currentPath));
                    String[] column_names = reader.readNext();
                    int num_of_columns = column_names.length;
                    String[] values = new String[num_of_columns];
                    values[0] = time;
                    values[1] = String.valueOf(cellId);
                    values[2] = lat + " ; " + lon;
                    if (Arrays.asList(column_names).contains(currentApp + upload) &&
                            Arrays.asList(column_names).contains(currentApp + download)) {
                        index_1 = Arrays.asList(column_names).indexOf(currentApp + upload);
                        index_2 = Arrays.asList(column_names).indexOf(currentApp + download);
                    }
                    for (int i = 3; i < num_of_columns; i++) {
                        if (index_1 == i)
                            values[i] = String.valueOf(upload_rate);
                        else if (index_2 == i)
                            values[i] = String.valueOf(download_rate);
                        else
                            values[i] = String.valueOf(0);
                    }


                    writer.writeNext(values);
                    //                   Log.d("Check", "Same App : " + currentFile.getName() + " exists. ");
                    reader.close();
                } else {
                    writer = new CSVWriter(new FileWriter(currentPath));
                    column_names.add(currentApp + upload);
                    column_names.add(currentApp + download);
                    writer.writeNext(column_names.toArray(new String[column_names.size()]));
                    //                   Log.d("Check", currentFile.getName() + " is created for the first time.");

                    mFirstFile = true;
                }

                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                if (currentFile.exists()) {
                    CSVReader reader = new CSVReader(new FileReader(currentPath));
                    String[] column_names = reader.readNext();
                    int i = 0;
                    if (Arrays.asList(column_names).contains(currentApp + upload)) {
                        //                       Log.d("Check", "Checking Column exists in the existing file. " + currentFile.getName() + "  " + currentApp);
                        writeOrReadFile(mFirstFile, true, time, cellId, lat, lon, currentApp, upload_rate, download_rate);
                        reader.close();
                    } else {
                        if (mFirstFile) {
                            currentFile = file1;
                            currentPath = tempFilePath;
                        } else {
                            //                           Log.d(tag, currentFile.getName() + " is deleted after new file is created and content is copied.");
                            currentFile = file;
                            currentPath = filePath;
                        }

                        if (currentFile.exists()) {
//                            Log.d(tag, currentFile.getName() + " exists in different app.");
                        } else {
                            if (mFirstFile) {
                                mFirstFile = false;
                            } else {
                                mFirstFile = true;
                                //                               Log.d(tag, currentFile.getName() + " is created.");
                            }

                            CSVWriter csvWriter = new CSVWriter(new FileWriter(currentPath));
                            String[] new_column_names = new String[column_names.length + 2];
                            for (i = 0; i < column_names.length; i++)
                                new_column_names[i] = column_names[i];
                            new_column_names[i++] = currentApp + upload;
                            new_column_names[i] = currentApp + download;
                            csvWriter.writeNext(new_column_names);
                            csvWriter.writeAll(reader.readAll());
                            int num_of_columns = new_column_names.length;
                            String[] values = new String[num_of_columns];
                            values[0] = time;
                            values[1] = String.valueOf(cellId);
                            values[2] = lat + " ; " + lon;

                            if (Arrays.asList(column_names).contains(currentApp + upload) &&
                                    Arrays.asList(column_names).contains(currentApp + download)) {
                                index_1 = Arrays.asList(column_names).indexOf(currentApp);
                                index_2 = Arrays.asList(column_names).indexOf(currentApp);
                            }
                            for (int j = 2; j < num_of_columns; j++) {
                                if (index_1 == j)
                                    values[j] = String.valueOf(upload_rate);
                                else if (index_2 == j)
                                    values[j] = String.valueOf(download_rate);
                                else
                                    values[j] = String.valueOf(0);
                            }
                            csvWriter.writeNext(values);

                            if (mFirstFile)
                                file1.delete();
                            else
                                file.delete();

                            csvWriter.close();
                            reader.close();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void createNotification(boolean samenotification, String contentText)   {

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, String.valueOf(CHANNEL_ID))
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("DRCollectApp")
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        Notification notification = mBuilder.build();
        notification.flags = Notification.FLAG_NO_CLEAR;

        notificationManager = NotificationManagerCompat.from(context);

        if(!notificationPresent) {
            notificationPresent = true;
            // notificationId is a unique int for each notification that you must define
            notificationManager.notify(notificationId, notification);
            notificationId++;
        }   else {
            if (samenotification) {
                // notificationId is a unique int for each notification that you must define
                notificationManager.notify(notificationId-1, notification);
            } else {
                notificationManager.notify(notificationId, notification);
                notificationId++;
            }
        }

    }


    private void sendBroadcastMessage()    {
        Intent intent = new Intent(ACTION_LOCATION_BROADCAST);

            intent.putExtra(EXTRA_LATITUDE, currentLat);
            intent.putExtra(EXTRA_LONGITUDE, currentLong);
            intent.putExtra("Cid", cell_id);
            intent.putExtra("RX",currentRXDataRate);
            intent.putExtra("TX", currentTXDataRate);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }



    private static class LocationListener implements android.location.LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            currentLat = location.getLatitude();
            currentLong = location.getLongitude();
        //    Log.d("Check", currentLat+"   :   "+currentLong+"  -  "+location.getProvider());
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
    //        Log.e(tag, "onStatusChanged: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider) {
    //        Log.e(tag, "onProviderEnabled: " + provider);
        }

        @Override
        public void onProviderDisabled(String provider) {
    //        Log.e(tag, "onProviderDisabled: " + provider);
        }
    }




}
