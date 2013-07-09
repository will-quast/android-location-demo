package com.williamquast.androidlocationdemo;


import android.app.Dialog;
import android.content.Context;
import android.content.IntentSender;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FusedLocationActivity extends FragmentActivity {

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private static final int ERROR_DIALOG_ON_CREATE_REQUEST_CODE = 4055;
    private static final int ERROR_DIALOG_ON_RESUME_REQUEST_CODE = 4056;
    private static final String TAG = "FusedLocationActivity";
    private static final int LOCATION_UPDATES_INTERVAL = 10000; // Setting 10 sec interval for location updates
    private Dialog errorDialog;
    private LocationClient locationClient;
    private LocationCallback mLocationCallback = new LocationCallback();
    private android.location.LocationListener gpsListener = new LocManagerListener();
    private android.location.LocationListener networkListener = new LocManagerListener();
    private Location lastLocation;
    private SupportMapFragment mapFragment;
    private GoogleMap googleMap;
    private TextView lastLocationText;
    private DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy H:mm:ss.SSS");
    private List<Marker> markers;
    private LocationManager locationManager;

    private CheckBox gpsCheckbox;
    private CheckBox networkCheckbox;
    private CheckBox fusedCheckbox;

    private ConnectionCallbacks connectionListener = new ConnectionCallbacks() {

        @Override
        public void onDisconnected() {
            Toast.makeText(FusedLocationActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onConnected(Bundle arg0) {
            Log.d(TAG, "connected!");
            Toast.makeText(FusedLocationActivity.this, "Connected", Toast.LENGTH_SHORT).show();
            Location location = locationClient.getLastLocation();
            if (location != null) {
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                handleLocation(location);
            }

            requestAllLocationUpdates();

        }
    };
    private OnConnectionFailedListener failedListener = new OnConnectionFailedListener() {

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            if (connectionResult.hasResolution()) {
                try {
                    // Start an Activity that tries to resolve the error
                    connectionResult.startResolutionForResult(FusedLocationActivity.this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
                    /*
                     * Thrown if Google Play services canceled the original PendingIntent
					 */
                } catch (IntentSender.SendIntentException e) {
                    // Log the error
                    e.printStackTrace();
                }
            } else {
                /*
				 * If no resolution is available, display a dialog to the user with the error.
				 */
                showErrorDialog(connectionResult);
            }
        }

    };

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fused_location);
        lastLocationText = (TextView) findViewById(R.id.lastLocationTextView);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        setupMap();
        setupRefreshButton();
        setupStopButton();
        setupStartButton();
        setupCheckboxes();
        checkGooglePlayServiceAvailability(ERROR_DIALOG_ON_CREATE_REQUEST_CODE);

    }

    private void setupCheckboxes() {
        gpsCheckbox = (CheckBox) findViewById(R.id.gpsCheckbox);
        gpsCheckbox.setTextColor(Color.CYAN);
        gpsCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, gpsListener);
                } else {
                    locationManager.removeUpdates(gpsListener);
                }
            }
        });

        networkCheckbox = (CheckBox) findViewById(R.id.networkCheckbox);
        networkCheckbox.setTextColor(Color.RED);
        networkCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, networkListener);
                } else {
                    locationManager.removeUpdates(networkListener);
                }
            }
        });

        fusedCheckbox = (CheckBox) findViewById(R.id.fusedCheckbox);
        fusedCheckbox.setTextColor(Color.rgb(160,120,240));
        fusedCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    requestFusedLocationUpdates();
                } else {
                    if (locationClient.isConnected()) {
                        locationClient.removeLocationUpdates(mLocationCallback);
                    }
                }
            }
        });
    }

    private void setupRefreshButton() {
        Button button = (Button) findViewById(R.id.refreshButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearMarkers();
            }
        });
    }

    private void setupStopButton() {
        Button button = (Button) findViewById(R.id.stopButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                removeAllLocationUpdates();
            }
        });
    }

    private void setupStartButton() {
        Button button = (Button) findViewById(R.id.startButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestAllLocationUpdates();
            }
        });
    }

    private void clearMarkers() {
        for (Marker marker : markers) {
            marker.remove();
        }
        markers.clear();
    }

    private void setupMap() {
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        googleMap = mapFragment.getMap();
        googleMap.setMyLocationEnabled(true);
        markers = new ArrayList<Marker>();

    }

    @Override
    public void onResume() {
        super.onResume();

        checkGooglePlayServiceAvailability(ERROR_DIALOG_ON_RESUME_REQUEST_CODE);
        restartLocationClient();
    }

    @Override
    public void onPause() {
        super.onPause();
        removeAllLocationUpdates();
        if (locationClient.isConnected()) {
            locationClient.disconnect();
        }

    }

    private void removeAllLocationUpdates() {
        if (locationClient.isConnected()) {
            locationClient.removeLocationUpdates(mLocationCallback);
        }
        locationManager.removeUpdates(gpsListener);
        locationManager.removeUpdates(networkListener);
        gpsCheckbox.setChecked(false);
        networkCheckbox.setChecked(false);
        fusedCheckbox.setChecked(false);

    }

    private void init() {
        if (locationClient == null) {
            locationClient = new LocationClient(this, connectionListener, failedListener);
            if (!(locationClient.isConnected() || locationClient.isConnecting())) {
                locationClient.connect();
            }
        }
    }

    private void restartLocationClient() {
        Log.d(TAG, "restartLocationClient");
        if (!(locationClient.isConnected() || locationClient.isConnecting())) {
            Log.d(TAG, "connecting!");
            locationClient.connect(); // Somehow it becomes connected here
            return;
        }
        Log.d(TAG, "already connected");
        requestAllLocationUpdates();
    }

    private void requestAllLocationUpdates() {
        requestFusedLocationUpdates();
        requestGpsLocationUpdates();
        requestGpsLocationUpdates();
        gpsCheckbox.setChecked(true);
        networkCheckbox.setChecked(true);
        fusedCheckbox.setChecked(true);
    }

    private void requestFusedLocationUpdates() {
        if (locationClient.isConnected()) {
            LocationRequest request = LocationRequest.create();
            request.setInterval(LOCATION_UPDATES_INTERVAL);
            request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationClient.requestLocationUpdates(request, mLocationCallback);
        }
    }

    private void requestGpsLocationUpdates() {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, gpsListener);
    }

    private void requestNetworkLocationUpdates() {
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, networkListener);
    }

    private void showErrorDialog(ConnectionResult connectionResult) {
        // Get the error code
        int errorCode = connectionResult.getErrorCode();
        // Get the error dialog from Google Play services
        Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(errorCode, this, CONNECTION_FAILURE_RESOLUTION_REQUEST);

        // If Google Play services can provide an error dialog
        if (errorDialog != null) {
            // Create a new DialogFragment for the error dialog
            ErrorDialogFragment errorFragment = new ErrorDialogFragment();
            // Set the dialog in the DialogFragment
            errorFragment.setDialog(errorDialog);
            // Show the error dialog in the DialogFragment
            errorFragment.show(getSupportFragmentManager(), "Location Updates");
        }
    }

    private void checkGooglePlayServiceAvailability(int requestCode) {
        // Query for the status of Google Play services on the device
        int statusCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getBaseContext());

        if (statusCode == ConnectionResult.SUCCESS) {
            Log.d(TAG, "play services success");
            init();
        } else {
            if (GooglePlayServicesUtil.isUserRecoverableError(statusCode)) {
                errorDialog = GooglePlayServicesUtil.getErrorDialog(statusCode,
                        this, requestCode);
                errorDialog.show();
            } else {
                Log.d(TAG, "unrecoverable play services error");
                // Handle unrecoverable error
            }
        }
    }

    private void handleLocation(Location location) {
        // Update the mLocationStatus with the lat/lng of the location
        Log.v(TAG, "LocationChanged == " + location.getProvider() + "@" + location.getLatitude() + "," + location.getLongitude());
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        double accuracy = location.getAccuracy();
        String provider = location.getProvider();
        String time = dateFormat.format(new Date(location.getTime()));
        lastLocationText.setText(String.format(getString(R.string.last_location), latitude, longitude, accuracy, provider, time));
        addMarker(location);
    }

    private void addMarker(Location location) {
        //String locationString = String.format(getString(R.string.location), location.getLatitude(),location.getLongitude());
        String accuracyString = String.format(getString(R.string.accuracy), location.getAccuracy());
        String timeString = dateFormat.format(new Date(location.getTime()));

        Marker marker = googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(location.getLatitude(), location.getLongitude()))
                .title(timeString)
                .snippet(accuracyString)
                .icon(BitmapDescriptorFactory.defaultMarker(getIconHue(location.getProvider()))));
        markers.add(marker);

    }

    private float getIconHue(String provider) {
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            return BitmapDescriptorFactory.HUE_CYAN;
        } else if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
            return BitmapDescriptorFactory.HUE_RED;
        } else if (provider.equals("fused")) {
            return BitmapDescriptorFactory.HUE_VIOLET;
        } else return BitmapDescriptorFactory.HUE_GREEN;
    }

    private class LocationCallback implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            if (location == null) {
                Log.v(TAG, "onLocationChanged: location == null");
                return;
            }
            if (!location.equals(lastLocation)) {
                handleLocation(location);
                lastLocation = location;
            }

        }
    }

    ;

    private class LocManagerListener implements android.location.LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            if (!location.equals(lastLocation)) {
                handleLocation(location);
                lastLocation = location;
            }
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    }

}
