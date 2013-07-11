package com.williamquast.androidlocationdemo;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.List;


public class WifiProximityService extends Service {

    // a TAG for logging
    private static final String TAG = "WifiProximityService";

    // the identity of the notification item
    private static final int TARGET_NOTIFICATION_ID = 1;

    // the Wifi SSID that we want to trigger the notification
    private static final String TARGET_SSID = "SEU-Guest";

    // a descriptive name for the wake lock we will take during updates
    private static final String WAKE_LOCK_TAG = "WifiProximityService";


    // the android wifi api
    private WifiManager wifiManager;

    // the android notification api
    private NotificationManager notificationManager;

    // the android power api
    private PowerManager powerManager;

    // broadcast listener for wifi scans
    private MyWifiScanReceiver wifiScanReceiver;

    // to to implement this but we dont need it
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;  // we want this instance to exists until we explicitly stop
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // ask android for the apis we need
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

        // make and register to receive broadcast about wifi scans
        wifiScanReceiver = new MyWifiScanReceiver();
        registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        Log.d(TAG, "started");
    }

    @Override
    public void onDestroy() {
        // the service is stopping to stop listening for broadcasts
        unregisterReceiver(wifiScanReceiver);

        Log.d(TAG, "stopped");
        super.onDestroy();
    }

    /**
     * A class to register to receive broadcast updates when a wifi scan completes
     */
    private class MyWifiScanReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // grab a wake lock so the cpu doesnt sleep before we are done working
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG);
            wakeLock.acquire();

            try {
                // get the latest scan results
                List<ScanResult> scan = wifiManager.getScanResults();

                for (ScanResult result : scan) {
                    // check if the ssid is a match
                    if (TARGET_SSID.equals(result.SSID)) {

                        // make a notification to present
                        Notification notification = new NotificationCompat.Builder(context)
                                .setContentTitle("Welcome to my place!")
                                .setContentText("Take a seat.")
                                .setSmallIcon(R.drawable.ic_launcher) // required
                                .build();

                        // present the notification, or do nothing if it already exists
                        notificationManager.notify(TARGET_NOTIFICATION_ID, notification);

                        Log.d(TAG, "notification");
                    }
                }
            } finally {

                // release the wake lock even if there was an exception
                if (wakeLock != null) {
                    wakeLock.release();
                }
            }
        }
    }
}
