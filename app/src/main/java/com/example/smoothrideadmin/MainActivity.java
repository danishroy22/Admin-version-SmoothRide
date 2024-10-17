package com.example.smoothrideadmin;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.Projection;

import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemClock;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.ToggleButton;


import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteFragment;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.text.Html;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.media.MediaPlayer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private DatabaseReference databaseReference;
    private Marker currentMarker;
    private MediaPlayer mediaPlayer;
    private List<Marker> markers = new ArrayList<>();
    private Button directions;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static float DISTANCE_THRESHOLD;
    private static final float INSTRUCTION_UPDATE_THRESHOLD = 50.0f;  // Threshold in meters for instruction updates
    private ToggleButton trackingToggle;
    private boolean isAutoCentering = true;
    private Set<Marker> nearbyMarkers = new HashSet<>();
    private LatLng destinationLatLng;
    private Marker searchMarker;
    private List<Polyline> polylineSegments = new ArrayList<>();
    private TextView instructionsTitle, LatDetails, LngDetails;
    private TextView instructionsText;
    double destLat, destLng;
    private int currentInstructionIndex = 0;
    private List<String> instructionsList = new ArrayList<>();
    private List<LatLng> stepsLatLng = new ArrayList<>();
    private final List<LatLng> waypoints = new ArrayList<>();
    private List<Waypoint> waypointsList = new ArrayList<>();
    private List<Polyline> polylines = new ArrayList<>();
    private Polyline traveledPolyline;
    private boolean isDrawingPolyline = false;
    private boolean isRerouting = false;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
    private static final int UPLOAD_REQUEST_CODE = 1001; // Unique code for upload request
    private MarkerCardController markerCardController;
    private FusedLocationProviderClient locationClient;
    private LatLng lastPosition;
    private List<LatLng> traveledPath = new ArrayList<>();
    private AutocompleteSupportFragment autocompleteFragment;
    String API_key = "AIzaSyCkA6VKSK-HprafZs-GSsfeGkOT1dpUxhE";


    //https://database2-c6ea1-default-rtdb.firebaseio.com
    //https://test-f6ef2-default-rtdb.firebaseio.com
    //https://dummydatabase-432f3-default-rtdb.firebaseio.com/
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        trackingToggle = findViewById(R.id.tracking_toggle);
        instructionsTitle = findViewById(R.id.instructions_title);
        instructionsText = findViewById(R.id.instructions_text);
        directions = findViewById(R.id.directions);
        ImageButton btnPrevious = findViewById(R.id.prev_button);
        ImageButton btnNext = findViewById(R.id.next_button);
        ImageButton Clear = findViewById(R.id.clear);
        SeekBar seekBar = findViewById(R.id.seekBar);
        drawerLayout = findViewById(R.id.drawerLayout);
        toggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        NavigationView navigationView = findViewById(R.id.navigation_menu);
        TextView latDetails = findViewById(R.id.LatitudeDetails);
        TextView lngDetails = findViewById(R.id.LongitudeDetails);
        markerCardController = new MarkerCardController(this, findViewById(R.id.rootView));

        if (!isUserLoggedIn()) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); //Remove MainActivity from the back stack
            return; // Stop executing the rest of the onCreate method
        }

        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize your ActionBar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        // Initialize MediaPlayer
        mediaPlayer = MediaPlayer.create(this, R.raw.notif_sound);
        if (mediaPlayer != null) {
            //mediaPlayer.start();
        } else {
            Log.e("MediaPlayerError", "MediaPlayer is not initialized");
        }
        // Initialize Firebase database
        databaseReference = FirebaseDatabase.getInstance().getReference("users");
        locationClient = LocationServices.getFusedLocationProviderClient(this);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseApp.initializeApp(this);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), API_key);
        }


        // Initialize the map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (mMap != null) {
            mMap.setTrafficEnabled(true);  // Enable traffic layer
        } else {
            Log.e("MainActivity", "GoogleMap object is null");
        }

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Define location callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Log.d("LocationUpdate", "Location callback received.");
                if (locationResult == null) {
                    Log.e("LocationError", "Location result is null.");
                    return;
                }

                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        Log.d("LocationUpdate", "Lat: " + location.getLatitude() + ", Lng: " + location.getLongitude());

                        if (currentMarker == null) {
                            currentMarker = mMap.addMarker(new MarkerOptions()
                                    .position(currentLocation)
                                    .title("You are here"));
                            Log.d("MarkerUpdate", "New marker added at current location.");
                        } else {
                            animateMarker(currentMarker, currentLocation);
                            Log.d("MarkerUpdate", "Marker position animated to new location.");
                        }

                        if (isDrawingPolyline) {
                            updateDynamicPolyline(currentLocation);
                            Log.d("PolylineUpdate", "Dynamic polyline updated.");
                        }

                        if (isAutoCentering) {
                            mMap.animateCamera(CameraUpdateFactory.newLatLng(currentLocation));
                            Log.d("CameraUpdate", "Camera animated to current location.");
                        }

                        if (isRerouting) {
                            fetchOptimizedRouteWithTraffic(currentLocation, destinationLatLng, waypoints);
                            Log.d("RouteUpdate", "Fetching optimized route with traffic.");
                        }

                        List<LatLng> polylinePoints = getPolylinePoints(); // Fetch the polyline points of the current road
                        checkProximity(currentLocation, polylinePoints);
                        Log.d("ProximityCheck", "Proximity to polyline checked.");

                        updateInstructionsBasedOnLocation(currentLocation);
                        Log.d("InstructionsUpdate", "Instructions updated based on new location.");
                    } else {
                        Log.e("LocationError", "Location object is null.");
                    }
                }
            }
        };


        Clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (traveledPolyline != null)
                {
                    traveledPolyline.remove();
                    traveledPolyline = null;
                    isDrawingPolyline = false;
                    traveledPath.clear();
                }
                if (polylineSegments != null) {
                    for (Polyline polyline : polylineSegments) {
                        polyline.remove(); // Remove each polyline from the map
                    }

                    polylineSegments.clear();

                    instructionsText.setText("");
                    isRerouting = false;
                    autocompleteFragment.setText("");

                    Toast.makeText(MainActivity.this, "Polylines cleared", Toast.LENGTH_SHORT).show();
                }
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                DISTANCE_THRESHOLD = 5 + (progress * 5);;

                Log.d("SeekBar", "Distance Threshold updated to: " + DISTANCE_THRESHOLD);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // Initialize AutocompleteSupportFragment
        autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        if (autocompleteFragment != null) {
            autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
            autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(@NonNull Place place) {
                    LatLng latLng = place.getLatLng();
                    if (latLng != null) {
                        destLat = latLng.latitude;
                        destLng = latLng.longitude;
                        destinationLatLng = latLng;

                        if (searchMarker != null) {
                            searchMarker.remove();
                        }
                        searchMarker = mMap.addMarker(new MarkerOptions().position(latLng).title(place.getName()));
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                    }
                }

                @Override
                public void onError(@NonNull Status status) {
                    Toast.makeText(MainActivity.this, "Error: " + status.getStatusMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Set up toggle switch listener
        trackingToggle.setOnCheckedChangeListener((buttonView, isChecked) -> isAutoCentering = isChecked);

        // Directions button listener
        directions.setOnClickListener(v -> {
            if (currentMarker != null && destinationLatLng != null) {
                LatLng origin = currentMarker.getPosition();
                LatLng destination = destinationLatLng;
                fetchOptimizedRouteWithTraffic(origin, destination, waypoints);
            } else {
                Toast.makeText(MainActivity.this, "Current location or destination is missing", Toast.LENGTH_SHORT).show();
            }
            isRerouting = true;
            isDrawingPolyline = true;
        });

        // Request location permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            startLocationUpdates();
        }

        btnPrevious.setOnClickListener(v -> {
            if (currentInstructionIndex > 0) {
                currentInstructionIndex--;  // Move to the previous instruction
                updateInstructionText();
            } else {
                // Optional: Disable the button or show a toast when at the first instruction
                Toast.makeText(this, "This is the first instruction", Toast.LENGTH_SHORT).show();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (currentInstructionIndex < instructionsList.size() - 1) {
                currentInstructionIndex++;  // Move to the next instruction
                updateInstructionText();
            } else {
                // Optional: Disable the button or show a toast when at the last instruction
                Toast.makeText(this, "You have reached the Destination", Toast.LENGTH_SHORT).show();
            }
        });

        // Show hamburger icon
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Handle navigation menu item clicks here.

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {

            @SuppressLint("NonConstantResourceId")
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                int itemId = menuItem.getItemId();

                if (itemId == R.id.menu_settings) {
                    Intent intent5 = new Intent(MainActivity.this, SettingsActivity.class);
                    startActivity(intent5);

                } else if (itemId == R.id.menu_About) {
                    Intent intent4 = new Intent(MainActivity.this, AboutUsActivity.class);
                    startActivity(intent4);

                } else if (itemId == R.id.menu_History) {
                    Intent intent6 = new Intent(MainActivity.this, HistoryLogActivity.class);
                    startActivity(intent6);

                } else if (itemId == R.id.menu_log_out) {
                    auth.signOut();
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(intent);
                }

                return false;
            }

        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (toggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private boolean isUserLoggedIn() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        return currentUser != null;
    }

    private List<LatLng> getPolylinePoints() {
        List<LatLng> allPoints = new ArrayList<>();
        // Iterate through all the polyline segments and collect their points
        for (Polyline polyline : polylineSegments) {
            List<LatLng> points = polyline.getPoints();
            allPoints.addAll(points);
        }

        return allPoints;
    }

    private void updateDynamicPolyline(LatLng currentLocation) {
        if (lastPosition == null) {
            lastPosition = currentLocation;
            return;
        }

        traveledPath.add(lastPosition);  // Start from the last position
        traveledPath.add(currentLocation);  // Add the current position

        if (traveledPolyline == null) {

            traveledPolyline = mMap.addPolyline(new PolylineOptions()
                    .addAll(traveledPath)
                    .color(Color.GREEN)
                    .width(10));
        } else {

            traveledPolyline.setPoints(traveledPath);
        }

        lastPosition = currentLocation;
    }

    private void fetchOptimizedRouteWithTraffic(LatLng currentLocation, LatLng destination, List<LatLng> waypoints) {
        StringBuilder waypointString = new StringBuilder();
        for (LatLng waypoint : waypoints) {
            waypointString.append(waypoint.latitude).append(",").append(waypoint.longitude).append("|");
        }

        if (waypointString.length() > 0) {
            waypointString.setLength(waypointString.length() - 1);
        }

        String url = "https://maps.googleapis.com/maps/api/directions/json?origin=" + currentLocation.latitude + "," + currentLocation.longitude +
                "&destination=" + destination.latitude + "," + destination.longitude +
                "&waypoints=optimize:true|" + waypointString +
                "&departure_time=now&traffic_model=best_guess&key="+API_key;

        new Thread(() -> {
            try {
                URL directionsUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) directionsUrl.openConnection();
                connection.setRequestMethod("GET");
                InputStream inputStream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject jsonResponse = new JSONObject(response.toString());
                JSONArray routes = jsonResponse.getJSONArray("routes");

                if (routes.length() > 0) {
                    JSONObject route = routes.getJSONObject(0);
                    JSONArray legs = route.getJSONArray("legs");
                    JSONObject leg = legs.getJSONObject(0);
                    JSONArray steps = leg.getJSONArray("steps");

                    waypointsList.clear();
                    instructionsList.clear();

                    runOnUiThread(() -> {
                        for (int i = 0; i < steps.length(); i++) {
                            try {
                                JSONObject step = steps.getJSONObject(i);
                                String polyline = step.getJSONObject("polyline").getString("points");
                                List<LatLng> points = decodePolyline(polyline);
                                // Create a new polyline for each segment
                                Polyline segment = mMap.addPolyline(new PolylineOptions()
                                        .addAll(points)
                                        .color(Color.BLUE)
                                        .width(10));
                                polylineSegments.add(segment); // Store the new segment
                                LatLng stepLocation = points.get(points.size() - 1);
                                String instruction = step.getString("html_instructions");

                                waypointsList.add(new Waypoint(stepLocation, Html.fromHtml(instruction).toString()));
                                instructionsList.add(Html.fromHtml(instruction).toString());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        currentInstructionIndex = 0;
                        updateInstructionText();
                    });
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to fetch route", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

    private void updateInstructionText() {
        if (!waypointsList.isEmpty() && currentInstructionIndex >= 0 && currentInstructionIndex < waypointsList.size()) {
            Waypoint currentWaypoint = waypointsList.get(currentInstructionIndex);
            instructionsText.setText(currentWaypoint.getInstruction());
        } else {
            instructionsText.setText("No instructions available.");
        }
    }
    private void updateInstructionsBasedOnLocation(LatLng currentLocation) {
        if (currentInstructionIndex < waypointsList.size()) {
            Waypoint nextWaypoint = waypointsList.get(currentInstructionIndex);
            LatLng nextStep = nextWaypoint.getLocation();

            float[] results = new float[1];
            Location.distanceBetween(currentLocation.latitude, currentLocation.longitude,
                    nextStep.latitude, nextStep.longitude, results);
            float distanceToNextStep = results[0];

            // Use a dedicated threshold for updating instructions
            if (distanceToNextStep < INSTRUCTION_UPDATE_THRESHOLD) {
                // Update instruction and increment index
                currentInstructionIndex++;

                if (currentInstructionIndex < waypointsList.size()) {
                    Waypoint currentWaypoint = waypointsList.get(currentInstructionIndex);
                    // Update the UI with the new instruction
                    instructionsText.setText(currentWaypoint.getInstruction());

                } else {
                    instructionsText.setText("You've reached your destination.");
                }
            }
        }
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        markerCardController.hideCard();
        fetchAndPlotMarkers();

        mMap.setOnMarkerClickListener(marker -> {
            markerCardController.showCard(marker);
            return true;
        });

        // Set a long click listener on the map
        mMap.setOnMapLongClickListener(latLng -> {
            // Remove existing marker if any
            if (searchMarker != null) {
                searchMarker.remove();
            }

            // Place a new marker at the long-pressed location
            searchMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("Destination"));

            // Update destination coordinates
            destLat = latLng.latitude;
            destLng = latLng.longitude;
            destinationLatLng = latLng;

            // Move the camera to the new marker
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));

            // Optionally, set the location in the autocomplete fragment if needed
            autocompleteFragment.setText(latLng.toString());
        });        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Delegate handling to MarkerCardController
        markerCardController.onActivityResult(requestCode, resultCode, data);
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 200)
                .setMinUpdateIntervalMillis(300)
                .build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }


    private void animateMarker(final Marker marker, final LatLng toPosition) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        Projection proj = mMap.getProjection();
        Point startPoint = proj.toScreenLocation(marker.getPosition());
        final LatLng startLatLng = proj.fromScreenLocation(startPoint);
        final long duration = 500;

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed / duration);
                double lng = t * toPosition.longitude + (1 - t) * startLatLng.longitude;
                double lat = t * toPosition.latitude + (1 - t) * startLatLng.latitude;
                marker.setPosition(new LatLng(lat, lng));

                if (t < 1.0) {
                    handler.postDelayed(this, 16);
                }
            }
        });
    }

    private void checkProximity(LatLng currentLocation, List<LatLng> polylinePoints) {
        for (Marker marker : markers) {
            if (marker.equals(currentMarker)) continue; // Skip the current location marker

            LatLng markerPosition = marker.getPosition();
            float[] results = new float[1];
            Location.distanceBetween(currentLocation.latitude, currentLocation.longitude,
                    markerPosition.latitude, markerPosition.longitude, results);

            float distanceInMeters = results[0];

            // Check if the marker is close to the polyline representing the current road
            if (distanceInMeters < DISTANCE_THRESHOLD && isMarkerNearPolyline(markerPosition, polylinePoints)) {
                if (!nearbyMarkers.contains(marker)) {
                    // The user is approaching this marker for the first time
                    nearbyMarkers.add(marker); // Mark this marker as nearby

                    if (mediaPlayer != null) {
                        if (mediaPlayer.isPlaying()) {
                            mediaPlayer.seekTo(0); // Reset to the beginning without stopping
                        }
                        mediaPlayer.start(); // Play notification sound
                        // Show the Toast when the sound plays
                        Toast.makeText(getApplicationContext(), "Pothole ahead", Toast.LENGTH_SHORT).show();

                    }
                }
            } else {
                nearbyMarkers.remove(marker); // Remove marker from the nearby set when leaving the proximity
            }
        }
    }

    // Helper method to check if a marker is near the polyline
    private boolean isMarkerNearPolyline(LatLng markerPosition, List<LatLng> polylinePoints) {
        float proximityThreshold = 10.0f; // Set a threshold for proximity to the polyline (e.g., 10 meters)

        for (int i = 0; i < polylinePoints.size() - 1; i++) {
            LatLng startPoint = polylinePoints.get(i);
            LatLng endPoint = polylinePoints.get(i + 1);

            // Check the perpendicular distance from the marker to this polyline segment
            float distance = getDistanceFromMarkerToSegment(markerPosition, startPoint, endPoint);

            // If the marker is close to any polyline segment, return true
            if (distance <= proximityThreshold) {
                return true;
            }
        }

        // If no segments are close to the marker, return false
        return false;
    }

    // Helper method to calculate the distance from the marker to a polyline segment
    private float getDistanceFromMarkerToSegment(LatLng marker, LatLng start, LatLng end) {
        float[] result = new float[1];

        // Calculate the distance from the marker to the line segment
        Location.distanceBetween(marker.latitude, marker.longitude, start.latitude, start.longitude, result);
        float startDistance = result[0];

        Location.distanceBetween(marker.latitude, marker.longitude, end.latitude, end.longitude, result);
        float endDistance = result[0];

        // Calculate the perpendicular distance from the marker to the segment
        Location.distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude, result);
        float segmentLength = result[0];

        if (segmentLength == 0) {
            return startDistance; // If the segment is a point, return the distance to the start
        }

        // Use the projection to calculate the perpendicular distance
        float t = (float) Math.max(0, Math.min(1, ((marker.latitude - start.latitude) * (end.latitude - start.latitude) +
                (marker.longitude - start.longitude) * (end.longitude - start.longitude)) /
                (segmentLength * segmentLength)
        ));

        LatLng projection = new LatLng(
                start.latitude + t * (end.latitude - start.latitude),
                start.longitude + t * (end.longitude - start.longitude)
        );

        // Calculate the distance from the marker to the projection point
        Location.distanceBetween(marker.latitude, marker.longitude, projection.latitude, projection.longitude, result);

        return result[0];
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void fetchAndPlotMarkers() {
        Log.e("MarkerFetch", "Fetching markers from Firebase");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.e("DataChange", "onDataChange called with snapshot: " + snapshot.getChildrenCount());

                if (mMap == null) {
                    Log.e("MapError", "GoogleMap object is null");
                    return;
                }

                mMap.clear();


                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    //Double lat = dataSnapshot.child("Latitude").getValue(Double.class);
                    //Double lng = dataSnapshot.child("Longitude").getValue(Double.class);
                    //String index = dataSnapshot.child("index").getValue(String.class);

                    Double lat = dataSnapshot.child("latitude").getValue(Double.class);
                    Double lng = dataSnapshot.child("longitude").getValue(Double.class);
                    String index = String.valueOf(dataSnapshot.child("pdi").getValue(Long.class));

                    if (lat == null) {
                        Log.e("DataError", "Latitude is null for entry: " + dataSnapshot.getKey());
                    }
                    if (lng == null) {
                        Log.e("DataError", "Longitude is null for entry: " + dataSnapshot.getKey());
                    }
                    if (index == null) {
                        Log.e("DataError", "Index (pdi) is null for entry: " + dataSnapshot.getKey());
                    }

                    Log.d("FirebaseData", "Lat: " + lat + ", Lng: " + lng + ", Index: " + index);

                    if (lat != null && lng != null && index != null) {
                        snapToRoad(lat, lng, index);
                    } else {
                        Log.e("DataError", "Invalid data: lat/lng/index is null");
                    }
                }
                Log.d("MarkerFetch", "Markers fetched and plotted successfully");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Failed to load data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("DatabaseError", "Failed to load data: " + error.getMessage());
            }
        });
    }
        private BitmapDescriptor getCustomMarkerIcon(int color) {
        int width = 15;
        int height = 15;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(width / 2, height / 2, width / 2, paint);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    // Define color constants
    private static final int COLOR_BLACK = Color.BLACK;
    private static final int COLOR_YELLOW = Color.YELLOW;
    private static final int COLOR_ORANGE = Color.rgb(255, 165, 0); // Custom orange
    private static final int COLOR_RED = Color.RED;

    private BitmapDescriptor getMarkerIcon(String index) {
        switch (index) {
            case "2":
                return getCustomMarkerIcon(COLOR_YELLOW);
            case "3":
                return getCustomMarkerIcon(COLOR_ORANGE);
            default:
                return getCustomMarkerIcon(COLOR_RED);
        }
    }

    private void snapToRoad(final double lat, final double lng, final String index) {
        String url = "https://roads.googleapis.com/v1/snapToRoads?path=" + lat + "," + lng + "&key="+API_key;

        new Thread(() -> {
            try {
                URL roadApiUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) roadApiUrl.openConnection();
                connection.setRequestMethod("GET");
                int responseCode = connection.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = connection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    // Parse the JSON response
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONArray snappedPoints = jsonResponse.getJSONArray("snappedPoints");

                    if (snappedPoints.length() > 0) {
                        JSONObject snappedPoint = snappedPoints.getJSONObject(0).getJSONObject("location");
                        final double snappedLat = snappedPoint.getDouble("latitude");
                        final double snappedLng = snappedPoint.getDouble("longitude");

                        // Plot the snapped point on the map
                        runOnUiThread(() -> {
                            LatLng snappedLatLng = new LatLng(snappedLat, snappedLng);
                            Marker marker = mMap.addMarker(new MarkerOptions()
                                    .position(snappedLatLng)
                                    .title("Lat: " + snappedLat + ", Lng: " + snappedLng)
                                    .icon(getMarkerIcon(index)));
                            markers.add(marker);
                            Log.d("SnapToRoad", "Marker added at: " + snappedLatLng);
                        });
                    } else {
                        Log.e("SnapToRoad", "No snapped points found");
                    }
                } else {
                    Log.e("SnapToRoad", "API response code: " + responseCode);
                }

            } catch (IOException | JSONException e) {
                Log.e("SnapToRoad", "Error in API call: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (locationCallback != null) {
            LocationServices.getFusedLocationProviderClient(this)
                    .removeLocationUpdates(locationCallback);
        }
    }
}
