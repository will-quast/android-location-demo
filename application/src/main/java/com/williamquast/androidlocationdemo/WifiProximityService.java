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

/**
 * Created by slacker on 6/30/13.
 */
public class WifiProximityService extends Service {

    private static final String TAG = "WifiProximityService";

    private static final int TARGET_NOTIFICATION_ID = 88;
    private static final String TARGET_SSID = "Slacker";

    private static final String WAKE_LOCK_TAG = "WifiProximityService";

    private Context context;
    private WifiManager wifiManager;
    private WifiScanReceiver wifiScanReceiver;
    private NotificationManager notificationManager;
    private PowerManager powerManager;

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

        wifiScanReceiver = new WifiScanReceiver();
        registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        Log.d(TAG, "started");
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(wifiScanReceiver);

        Log.d(TAG, "stopped");
        super.onDestroy();
    }

    private class WifiScanReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG);
            wakeLock.acquire();
            try {
                List<ScanResult> scan = wifiManager.getScanResults();
                for (ScanResult result : scan) {
                    if (TARGET_SSID.equals(result.SSID)) {
                        Notification notification = new NotificationCompat.Builder(context)
                                .setContentTitle("Welcome to my place!")
                                .setContentText("Take a seat.")
                                .setSmallIcon(R.drawable.ic_launcher) // required
                                .build();
                        notificationManager.notify(TARGET_NOTIFICATION_ID, notification);
                        Log.d(TAG, "notification");
                    }
                    Log.d(TAG, "### ssid="+result.SSID);
                }
            } finally {
                if (wakeLock != null) {
                    wakeLock.release();
                }
            }
        }
    }
}
