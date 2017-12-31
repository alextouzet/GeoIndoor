package com.example.alexandre.geoindoor;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.lize.oledcomm.camera_lifisdk_android.ILiFiPosition;
import com.lize.oledcomm.camera_lifisdk_android.LiFiSdkManager;
import com.lize.oledcomm.camera_lifisdk_android.V1.LiFiCamera;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private LiFiSdkManager liFiSdkManager;
    private SupportMapFragment fragment;
    String name = "";

    private boolean loaded = false;

    LocationManager mLocationManager;

    //K: ID, V: Name
    List<String> names = new ArrayList<>();
    List<String> ids = new ArrayList<>();
    List<Marker> markers = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
    }

    protected void onStart() {
        super.onStart();


        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference users = database.getReference("users");

        SharedPreferences prefs = getSharedPreferences("id", 0);

        if (!prefs.contains("id")){
            SharedPreferences.Editor mEditor = prefs.edit();
            mEditor.putString("id", UUID.randomUUID().toString()).commit();
        }

        final String token = prefs.getString("id", null);

        ListView mFriendsList = findViewById(R.id.friendsList);
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, names);
        mFriendsList.setAdapter(adapter);

        users.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshot) {

                if (!dataSnapshot.hasChild(token)) {
                    addUserToDB(database, token);
                }
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
                }
                else
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
                // Get the selected item text from ListView
                String selectedItem = (String) parent.getItemAtPosition(position);
                Log.d("BUTTONCLICK", ids.get(position));
                DatabaseReference messageRef = database.getReference("messages");
                messageRef.push().setValue(new Message(ids.get(position), token, "Envoie ta position stp", "De la part de: " + name));
            }
        });
        subscribeToTopic();

    }

    protected void onResume() {
        super.onResume();

        //CREATION DE LA CARTE GOOGLE

        fragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        fragment.getMapAsync(this);

        //LIFI

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        1);
            }
        }

        liFiSdkManager = new LiFiSdkManager(this, LiFiSdkManager.CAMERA_LIB_VERSION_0_1,
                "token", "user", new ILiFiPosition() {
            @Override
            public void onLiFiPositionUpdate(String lamp) {
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
        liFiSdkManager.start();
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
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
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
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        1);
            }
        }

        mLocationManager = (LocationManager)getApplicationContext().getSystemService(LOCATION_SERVICE);
        List<String> providers = mLocationManager.getProviders(true);
        Location bestLocation = null;

        for (String provider : providers) {
            Location l = mLocationManager.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                // Found best last known location:
                bestLocation = l;
            }
        }
        if(bestLocation!=null) {
            double longitude = bestLocation.getLongitude();
            double latitude = bestLocation.getLatitude();

            googleMap.addMarker(new MarkerOptions()
                    .position(new LatLng(latitude, longitude))
                    .title("Votre position")
                    .snippet(latitude+", "+longitude)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 14));
        }
    }

    public void subscribeToTopic() {
        FirebaseMessaging.getInstance().subscribeToTopic("notifications");
        Log.d("Subscribe to","notifications");
    }
}