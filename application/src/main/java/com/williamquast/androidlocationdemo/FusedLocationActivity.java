package com.williamquast.androidlocationdemo;


import android.app.Dialog;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

public class FusedLocationActivity extends FragmentActivity {

	private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
	private static final int ERROR_DIALOG_ON_CREATE_REQUEST_CODE = 4055;
	private static final int ERROR_DIALOG_ON_RESUME_REQUEST_CODE = 4056;
	
	private Dialog errorDialog;
	private LocationClient locationClient;
	private LocationCallback mLocationCallback = new LocationCallback();
    private static final String TAG = "FusedLocationActivity";
    private static final int LOCATION_UPDATES_INTERVAL = 10000; // Setting 10 sec interval for location updates
    
	
	
	 /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fused_location);
        
        checkGooglePlayServiceAvailability(ERROR_DIALOG_ON_CREATE_REQUEST_CODE);
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
        
        if (locationClient.isConnected()) {
        	locationClient.removeLocationUpdates(mLocationCallback);
        	locationClient.disconnect();
        }
    }
    
    private void init() {
        
        
        if (locationClient == null) {
	        locationClient = new LocationClient(this, mLocationCallback, mLocationCallback);
	        if (!(locationClient.isConnected() || locationClient.isConnecting())) {
	        	locationClient.connect();
	        }
        }
    }
    
    private void restartLocationClient() {
    	if (!(locationClient.isConnected() || locationClient.isConnecting())) {
            locationClient.connect(); // Somehow it becomes connected here
            return;
        }
        LocationRequest request = LocationRequest.create();
        request.setInterval(LOCATION_UPDATES_INTERVAL);
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationClient.requestLocationUpdates(request, mLocationCallback);
    }
	
	private void showErrorDialog(ConnectionResult connectionResult) {
		// Get the error code
        int errorCode = connectionResult.getErrorCode();
        // Get the error dialog from Google Play services
        Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(errorCode, this, CONNECTION_FAILURE_RESOLUTION_REQUEST);

        // If Google Play services can provide an error dialog
        if (errorDialog != null) {
            // Create a new DialogFragment for the error dialog
            ErrorDialogFragment errorFragment =  new ErrorDialogFragment();
            // Set the dialog in the DialogFragment
            errorFragment.setDialog(errorDialog);
            // Show the error dialog in the DialogFragment
            errorFragment.show(getSupportFragmentManager(), "Location Updates");
        }
	}
	
	private void checkGooglePlayServiceAvailability(int requestCode) {
		// Query for the status of Google Play services on the device
		int statusCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(getBaseContext());

		if (statusCode == ConnectionResult.SUCCESS) {
			init();
		} else {
			if (GooglePlayServicesUtil.isUserRecoverableError(statusCode)) {
				errorDialog = GooglePlayServicesUtil.getErrorDialog(statusCode,
						this, requestCode);
				errorDialog.show();
			} else {
				// Handle unrecoverable error
			}
		}
	}

	private ConnectionCallbacks connectionListener = new ConnectionCallbacks() {

		@Override
		public void onDisconnected() {
			Toast.makeText(FusedLocationActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onConnected(Bundle arg0) {
			Toast.makeText(FusedLocationActivity.this, "Connected", Toast.LENGTH_SHORT).show();
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
	
	private class LocationCallback implements ConnectionCallbacks, OnConnectionFailedListener,
    LocationListener {
			
    @Override
    public void onConnected(Bundle connectionHint) {
    	
    	// Display last location
    	Location location = locationClient.getLastLocation();
    	if (location != null) {
    	    handleLocation(location);
    	}
    	
    	// Request for location updates
        LocationRequest request = LocationRequest.create();
        request.setInterval(LOCATION_UPDATES_INTERVAL);
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationClient.requestLocationUpdates(request, mLocationCallback);
    }
    
    @Override
    public void onDisconnected() {
    	Log.v(TAG, "Location Client disconnected by the system");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
    	Log.v(TAG, "Location Client connection failed");
    }
    
    @Override
    public void onLocationChanged(Location location) {
        if (location == null) {
            Log.v(TAG, "onLocationChanged: location == null");
            return;
        }
        
        handleLocation(location);
    }
    
	private void handleLocation(Location location) {
		// Update the mLocationStatus with the lat/lng of the location
		Log.v(TAG, "LocationChanged == @" +
                location.getLatitude() + "," + location.getLongitude());
       
	}
};

}
