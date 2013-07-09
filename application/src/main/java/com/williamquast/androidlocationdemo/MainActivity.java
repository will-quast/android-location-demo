package com.williamquast.androidlocationdemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {

    private Button fusedLocationButton;
    private Button gpsLocationButton;
    private Button wifiProximityButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        fusedLocationButton = (Button) findViewById(R.id.fusedLocation);
        fusedLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getBaseContext(), FusedLocationActivity.class));
            }
        });

        wifiProximityButton = (Button) findViewById(R.id.wifiProximity);
        wifiProximityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getBaseContext(), WifiProximityActivity.class));
            }
        });

        gpsLocationButton = (Button) findViewById(R.id.gpsLocation);
        gpsLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getBaseContext(), GpsLocationActivity.class));
            }
        });
    }
}
