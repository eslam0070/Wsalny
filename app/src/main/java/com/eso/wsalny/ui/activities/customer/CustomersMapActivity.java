package com.eso.wsalny.ui.activities.customer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.bumptech.glide.Glide;
import com.eso.wsalny.R;
import com.eso.wsalny.ui.activities.SettingsActivity;
import com.eso.wsalny.ui.activities.WelcomeActivity;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

@SuppressLint("SetTextI18n")
public class CustomersMapActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    GoogleMap mMap;
    GoogleApiClient googleApiClient;
    Location LastLocation;
    LocationRequest locationRequest;
    FirebaseAuth mAuth;
    FirebaseUser currentUser;
    String customerID, driverFoundID;
    Button mCustomerSetting, mCustomerLogout, mCustomerCallCarButton;
    DatabaseReference CustomerDatabaseRef, DriverAvailableRef, DriversRef, DriverLocationRef;
    LatLng CustomerPickUpLocation;
    int radius = 1;
    Boolean driverFound = false, requestType = false;
    Marker DriverMarker, PickUpMarker;
    ValueEventListener DriverLocationRefListener;
    GeoQuery geoQuery;
    RelativeLayout relativeLayout;
    CircleImageView profileImageDriver;
    TextView nameDriver,phoneDriver,carNameDriver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customers_map);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        customerID = currentUser.getUid();
        CustomerDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Customer Requests");
        DriverAvailableRef = FirebaseDatabase.getInstance().getReference().child("Drivers Available");
        DriverLocationRef = FirebaseDatabase.getInstance().getReference().child("Drivers Working");
        mCustomerSetting = findViewById(R.id.customer_setting);
        mCustomerSetting.setOnClickListener(v -> {
            Intent intent = new Intent(CustomersMapActivity.this, SettingsActivity.class);
            intent.putExtra("type", "Customers");
            startActivity(intent);
        });
        mCustomerLogout = findViewById(R.id.customer_logout);
        mCustomerLogout.setOnClickListener(v -> {
            mAuth.signOut();
            LogOutUser();
        });
        mCustomerCallCarButton = findViewById(R.id.customer_call_car_button);
        mCustomerCallCarButton.setOnClickListener(v -> {
            if (requestType) {
                requestType = false;
                geoQuery.removeAllListeners();
                DriverLocationRef.removeEventListener(DriverLocationRefListener);

                if (driverFound != null) {
                    DriversRef = FirebaseDatabase.getInstance().getReference()
                            .child("Users").child("Drivers").child(driverFoundID).child("CustomerRideID");

                    DriversRef.removeValue();

                    driverFoundID = null;

                }

                driverFound = false;
                radius = 1;

                String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                GeoFire geoFire = new GeoFire(CustomerDatabaseRef);
                geoFire.removeLocation(customerId, (key, error) -> {
                });

                if (PickUpMarker != null) {
                    PickUpMarker.remove();
                }
                if (DriverMarker != null) {
                    DriverMarker.remove();
                }

                mCustomerCallCarButton.setText("Call a Cab");
                relativeLayout.setVisibility(View.GONE);

            } else {
                requestType = true;

                String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                GeoFire geoFire = new GeoFire(CustomerDatabaseRef);
                geoFire.setLocation(customerId, new GeoLocation(LastLocation.getLatitude(), LastLocation.getLongitude()), (key, error) -> {
                });

                CustomerPickUpLocation = new LatLng(LastLocation.getLatitude(), LastLocation.getLongitude());
                PickUpMarker = mMap.addMarker(new MarkerOptions().position(CustomerPickUpLocation).title("My Location").icon(BitmapDescriptorFactory.fromResource(R.drawable.user)));

                mCustomerCallCarButton.setText("Getting your Driver...");
                getClosetDriverCab();
            }
        });

        relativeLayout = findViewById(R.id.rell);
        profileImageDriver = findViewById(R.id.profile_image_driver);
        nameDriver = findViewById(R.id.name_driver);
        phoneDriver = findViewById(R.id.phone_driver);
        carNameDriver = findViewById(R.id.car_name_driver);
    }

    private void getClosetDriverCab() {
        GeoFire geoFire = new GeoFire(DriverAvailableRef);
        geoQuery = geoFire.queryAtLocation(new GeoLocation
                (CustomerPickUpLocation.latitude, CustomerPickUpLocation.longitude), radius);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!driverFound && requestType) {
                    driverFound = true;
                    driverFoundID = key;
                    DriversRef = FirebaseDatabase.getInstance().getReference()
                            .child("Users").child("Drivers").child(driverFoundID);
                    HashMap driverMap = new HashMap();
                    driverMap.put("CustomerRideID", customerID);
                    DriversRef.updateChildren(driverMap);
                    GettingDriverLocation();
                    mCustomerCallCarButton.setText("Locking for Driver Location...");
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (!driverFound) {
                    radius = radius + 1;
                    getClosetDriverCab();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private void GettingDriverLocation() {
        DriverLocationRefListener = DriverLocationRef.child(driverFoundID).child("l")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            List<Object> driverLocationMap = (List<Object>) dataSnapshot.getValue();
                            double LocationLat = 0;
                            double LocationLng = 0;
                            mCustomerCallCarButton.setText("Driver Found");
                            relativeLayout.setVisibility(View.VISIBLE);
                            getAssignedDriverInformation();
                            if (driverLocationMap.get(0) != null) {
                                LocationLat = Double.parseDouble(driverLocationMap.get(0).toString());
                            }
                            if (driverLocationMap.get(1) != null) {
                                LocationLng = Double.parseDouble(driverLocationMap.get(1).toString());
                            }

                            //adding marker - to pointing where driver is - using this lat lng
                            LatLng DriverLatLng = new LatLng(LocationLat, LocationLng);
                            if (DriverMarker != null) {
                                DriverMarker.remove();
                            }

                            Location location1 = new Location("");
                            location1.setLatitude(CustomerPickUpLocation.latitude);
                            location1.setLongitude(CustomerPickUpLocation.longitude);

                            Location location2 = new Location("");
                            location2.setLatitude(DriverLatLng.latitude);
                            location2.setLongitude(DriverLatLng.longitude);

                            float Distance = location1.distanceTo(location2);

                            if (Distance < 90) {
                                mCustomerCallCarButton.setText("Driver's Reached");
                            } else {
                                mCustomerCallCarButton.setText("Driver Found: " + String.valueOf(Distance));
                            }

                            DriverMarker = mMap.addMarker(new MarkerOptions().position(DriverLatLng)
                                    .title("your driver is here").icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //

            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        LastLocation = location;
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(12));

    }

    protected synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
        googleApiClient.connect();
    }

    private void getAssignedDriverInformation(){
        DatabaseReference reference = FirebaseDatabase.getInstance()
                .getReference().child("Users").child("Drivers").child(driverFoundID);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    String name = "" + dataSnapshot.child("edtName").getValue();
                    String phone = "" + dataSnapshot.child("phone").getValue();
                    String car = "" + dataSnapshot.child("car").getValue();

                    nameDriver.setText(name);
                    phoneDriver.setText(phone);
                    carNameDriver.setText(car);

                    if (dataSnapshot.hasChild("image")) {
                        String image = "" + dataSnapshot.child("image").getValue();
                        Glide.with(CustomersMapActivity.this).load(image).placeholder(R.drawable.profile).into(profileImageDriver);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    @Override
    protected void onStop() {
        super.onStop();
    }

    private void LogOutUser() {
        startActivity(new Intent(CustomersMapActivity.this, WelcomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

}
