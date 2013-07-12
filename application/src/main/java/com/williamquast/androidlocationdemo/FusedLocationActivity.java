package com.williamquast.androidlocationdemo;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentSender;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
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
    private boolean providersEnabled;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fused_location);
        lastLocationText = (TextView) findViewById(R.id.lastLocationTextView);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if(haveLocationProvider()) {
            providersEnabled = true;
            setupMap();
            setupRefreshButton();
            setupStopButton();
            setupStartButton();
            setupCheckboxes();
            checkGooglePlayServiceAvailability(ERROR_DIALOG_ON_CREATE_REQUEST_CODE);
        } else {
            providersEnabled = false;
            handleNoProvider();
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        if(providersEnabled) {
            checkGooglePlayServiceAvailability(ERROR_DIALOG_ON_RESUME_REQUEST_CODE);
            restartLocationClient();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if(providersEnabled) { //user has not disabled Location Providers in Settings
            removeAllLocationUpdates();
            if (locationClient.isConnected()) {
                locationClient.disconnect();
            }
        }

    }

    private void removeAllLocationUpdates() {
        if (locationClient.isConnected()) { //verify that locationClient is connected and remove updates
            locationClient.removeLocationUpdates(mLocationCallback);
        }

        //remove updates from location manager
        locationManager.removeUpdates(gpsListener);
        locationManager.removeUpdates(networkListener);

        gpsCheckbox.setChecked(false);
        networkCheckbox.setChecked(false);
        fusedCheckbox.setChecked(false);

    }

    private void init() {
        //initializing, connecting locationClient
        if (locationClient == null) {
            locationClient = new LocationClient(this, connectionListener, failedListener);
            if (!(locationClient.isConnected() || locationClient.isConnecting())) {
                locationClient.connect();
            }
        }
    }

    private void restartLocationClient() {
        if (!(locationClient.isConnected() || locationClient.isConnecting())) {
            Log.d(TAG, "connecting!");
            locationClient.connect();
            return;
        }
        //already connected, ok to request updates
        requestAllLocationUpdates();
    }

    private void requestAllLocationUpdates() {
        requestFusedLocationUpdates();
        requestGpsLocationUpdates();
        requestNetworkLocationUpdates();
        gpsCheckbox.setChecked(true);
        networkCheckbox.setChecked(true);
        fusedCheckbox.setChecked(true);
    }

    private void requestGpsLocationUpdates() {
        //request gps updates with no minimum movement time requirements
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, gpsListener);
    }

    private void requestNetworkLocationUpdates() {
        //request gps updates with no minimum movement time requirements
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, networkListener);
    }

    private void requestFusedLocationUpdates() {
        // verify location client is connected, then request updates
        if (locationClient.isConnected()) {
            LocationRequest request = LocationRequest.create()
                    .setInterval(LOCATION_UPDATES_INTERVAL)
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationClient.requestLocationUpdates(request, mLocationCallback);
        }
    }





    private void checkGooglePlayServiceAvailability(int requestCode) {
        // Query for the status of Google Play services on the device
        int statusCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getBaseContext());

        if (statusCode == ConnectionResult.SUCCESS) {
            //GooglePlayService available, initialize locationClient
            init();
        } else {
            if (GooglePlayServicesUtil.isUserRecoverableError(statusCode)) {
                //play service has a recoverable error, show dialog
                errorDialog = GooglePlayServicesUtil.getErrorDialog(statusCode,
                        this, requestCode);
                errorDialog.show();
            } else {
                Log.d(TAG, "unrecoverable play services error");
            }
        }
    }

    private void handleLocation(Location location) {
        // Update the mLocationStatus with the lat/lng of the location
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        double accuracy = location.getAccuracy();
        String provider = location.getProvider();
        String time = dateFormat.format(new Date(location.getTime()));
        lastLocationText.setText(String.format(getString(R.string.last_location), latitude, longitude, accuracy, provider, time));
        addMarker(location);
    }

    private boolean haveLocationProvider() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private void handleNoProvider() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.no_location_provider_enabled_title)
                .setMessage(R.string.no_location_provider_enabled_message)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                });
        builder.show();
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
        fusedCheckbox.setTextColor(Color.rgb(160, 120, 240));
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


    /**
     * Handles connection callbacks
     */
    private ConnectionCallbacks connectionListener = new ConnectionCallbacks() {

        @Override
        public void onDisconnected() {
            //play services disconnected
            Toast.makeText(FusedLocationActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onConnected(Bundle arg0) {
            //play services connected
            Log.d(TAG, "connected!");
            Toast.makeText(FusedLocationActivity.this, "Connected", Toast.LENGTH_SHORT).show();

            //connected, move map to last known location
            Location location = locationClient.getLastLocation();
            if (location != null) {
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                handleLocation(location);
            }

            //request location updates
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
                } catch (IntentSender.SendIntentException e) {
                    // Log the error
                    e.printStackTrace();
                }
            } else {
                 // If no resolution is available, display a dialog to the user with the error.
				showErrorDialog(connectionResult);
            }
        }

    };

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


}
