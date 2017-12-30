package com.example.alexandre.geoindoor;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.lize.oledcomm.camera_lifisdk_android.ILiFiPosition;
import com.lize.oledcomm.camera_lifisdk_android.LiFiSdkManager;
import com.lize.oledcomm.camera_lifisdk_android.V1.LiFiCamera;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private LiFiSdkManager liFiSdkManager;
    private SupportMapFragment fragment;
    private String m_Text = "";

    LocationManager mLocationManager;

    List<String> nameUsers = new ArrayList<>();
    List<String> idUsers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
    }

    protected void onStart() {
        super.onStart();
    }

    protected void onResume() {
        super.onResume();

        DatabaseReference users;
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        users = database.getReference("users");

        users.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshot) {

                for (DataSnapshot dsp : dataSnapshot.getChildren()) {
                    String name = dsp.child("name").getValue().toString();
                    String id = dsp.child("id").getValue().toString();
                    addToFriendsList(name, id);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

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

    private void addToFriendsList(String name, String id) {
        ListView mFriendsList = findViewById(R.id.friendsList);

        nameUsers.add(name);
        idUsers.add(id);

        //PARTIE AFFICHAGE

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, nameUsers);

        mFriendsList.setAdapter(adapter);

        mFriendsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Get the selected item text from ListView
                String selectedItem = (String) parent.getItemAtPosition(position);
                Log.d("BUTTONCLICK", idUsers.get(position));
            }
        });
    }

    private String nameDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Title");

// Set up the input
        final EditText input = new EditText(this);
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

// Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                m_Text = input.getText().toString();
            }
        });

        builder.show();
        return m_Text;
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
                    .title("Votre position"));

            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 15));
        }
    }
}