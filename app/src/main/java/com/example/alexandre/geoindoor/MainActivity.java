package com.example.alexandre.geoindoor;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.lize.oledcomm.camera_lifisdk_android.LiFiSdkManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private LiFiSdkManager liFiSdkManager;

    private String detectedLamp, lamp = "";
    private Double longitude, latitude;
    private SupportMapFragment fragment;
    private String name = "";
    private GoogleMap map;

    LocationRequest mLocationRequest;
    GoogleApiClient googleApiClient;
    Location mLastLocation;
    Marker mCurrLocationMarker;

    List<String> names = new ArrayList<>();
    List<String> ids = new ArrayList<>();
    List<Pair<String, MarkerOptions>> markers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        SharedPreferences prefs = getSharedPreferences("id", 0);

        if (!prefs.contains("id")) {
            SharedPreferences.Editor mEditor = prefs.edit();
            mEditor.putString("id", UUID.randomUUID().toString()).commit();
        }

        final String token = prefs.getString("id", null);

        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference users = database.getReference("users");

        ListView mFriendsList = findViewById(R.id.friendsList);
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, names);
        mFriendsList.setAdapter(adapter);

        users.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshot) {
                if (!dataSnapshot.hasChild(token))
                    addUserToDB(database, token);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        users.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                GoogleMap googleMap = null;
                String currname = dataSnapshot.child("name").getValue().toString();
                if (!dataSnapshot.getKey().equals(token)) {
                    names.add(currname);
                    ids.add(dataSnapshot.getKey());
                    adapter.notifyDataSetChanged();
                } else
                    name = currname;
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                String name = dataSnapshot.child("name").getValue().toString();
                names.remove(name);
                ids.remove(dataSnapshot.getKey());
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mFriendsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DatabaseReference messageRef = database.getReference("messages");
                Message message = new Message(ids.get(position), token, "Demande de position", "De la part de: " + name);
                messageRef.push().setValue(message);
            }
        });

        subscribeToTopic();

        fragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        fragment.getMapAsync(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        1);
            }
        }

        /*liFiSdkManager = new LiFiSdkManager(this, LiFiSdkManager.CAMERA_LIB_VERSION_0_1,
                "token", "user", new ILiFiPosition() {
            @Override
            public void onLiFiPositionUpdate(String lamp) {
                detectedLamp = lamp;
                TextView locationStatus = findViewById(R.id.locationStatus);
                if(lamp.contains("No lamp detected")){
                    locationStatus.setText("Scannez une lampe avec l'appareil frontal...");
                } else {
                    locationStatus.setText("Numéro lampe : "+ lamp);
                }
            }
        });

        liFiSdkManager.setLocationRequestMode(LiFiSdkManager.LOCATION_REQUEST_OFFLINE_MODE);
        liFiSdkManager.init(R.id.Layout, LiFiCamera.FRONT_CAMERA);
        liFiSdkManager.start();*/

    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getExtras() != null) {
            Log.d("onNewIntent", intent.getExtras().keySet().toString());
            if (intent.getExtras().get("asked").equals("false")) {

                DatabaseReference messageRef = FirebaseDatabase.getInstance().getReference("messages");
                Message message = new Message((String) intent.getExtras().get("sender"), (String) intent.getExtras().get("receiver"), "Voici ma position", "Signé: " + name, lamp, latitude, longitude);
                messageRef.push().setValue(message);
            } else {
                Double latitude = Double.parseDouble((String) intent.getExtras().get("latitude"));
                Double longitude = Double.parseDouble((String) intent.getExtras().get("longitude"));
                MarkerOptions marker = new MarkerOptions()
                        .position(new LatLng(latitude, longitude))
                        .title((String) intent.getExtras().get("message"))
                        .snippet(latitude + ", " + longitude);
                String sender = (String) intent.getExtras().get("sender");
                Log.d("presque", sender);
                for (int i = 0; i < markers.size(); i++) {
                    Log.d("presque" + i, markers.get(i).first);
                    if (markers.get(i).first.equals(sender)) {
                        markers.get(i).second.visible(false);
                        markers.remove(i);
                    }
                }
                markers.add(new Pair(sender, marker));
                map.addMarker(marker);

                map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 14));
            }
        }
    }

    protected void onResume() {
        super.onResume();
    }

    private void addUserToDB(final FirebaseDatabase database, final String token) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Entrez votre nom");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                name = input.getText().toString();
                DatabaseReference myRef = database.getReference("users/" + token);
                myRef.child("name").setValue(name);
            }
        });

        builder.show();
    }

    protected void onPause() {
        super.onPause();
        if (liFiSdkManager != null && liFiSdkManager.isStarted()) {
            liFiSdkManager.stop();
            liFiSdkManager.release();
            liFiSdkManager = null;
        }
        if (googleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
        }
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        this.map = googleMap;

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                googleMap.setMyLocationEnabled(true);
            }
            else
                checkLocationPermission();
    }

    protected synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("onLocationChanged", location.toString());
        mLastLocation = location;
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude),14));
    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (googleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        map.setMyLocationEnabled(true);
                    }

                }
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    public void subscribeToTopic() {
        FirebaseMessaging.getInstance().subscribeToTopic("notifications");
        Log.d("Subscribe to", "notifications");
    }

}