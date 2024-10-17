package com.example.smoothrideadmin;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.model.Marker;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class MarkerCardController {
    private static final int REQUEST_CAMERA = 1888;
    private static final int REQUEST_GALLERY = 1889;
    private static final int PERMISSION_REQUEST_CODE = 1001;

    private Context context;
    private CardView markerCard;
    private TextView LatDetails, LngDetails;
    private ImageView selectedImage;
    private Button uploadButton, cameraButton, galleryButton, repairButton, cancelButton;
    private Uri imageUri;
    private Marker currentMarker;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private DatabaseReference uploadDatabaseRef;
    private HashMap<String, Uri> markerImageMap;
    private DatabaseHelper databaseHelper;
    private Bitmap selectedImageBitmap;

    public MarkerCardController(Context context, View rootView) {
        this.context = context;
        databaseHelper = new DatabaseHelper(context);
        markerImageMap = new HashMap<>();
        storage = FirebaseStorage.getInstance("gs://dummydatabase-432f3.appspot.com");
        storageRef = storage.getReference();

        initializeViews(rootView);
        requestPermissions();
        setupListeners();
    }

    private void initializeViews(View rootView) {
        markerCard = rootView.findViewById(R.id.markerCard);
        LatDetails = rootView.findViewById(R.id.LatitudeDetails);
        LngDetails = rootView.findViewById(R.id.LongitudeDetails);
        selectedImage = rootView.findViewById(R.id.selectedImage);
        uploadButton = rootView.findViewById(R.id.uploadButton);
        cameraButton = rootView.findViewById(R.id.CameraButton);
        galleryButton = rootView.findViewById(R.id.GalleryButton);
        repairButton = rootView.findViewById(R.id.repairButton);
        cancelButton = rootView.findViewById(R.id.cancelButton);
    }

    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, Manifest.permission.CAMERA) ||
                    ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                showToast("Camera and storage permissions are needed for the app to function properly.");
            }

            ActivityCompat.requestPermissions((Activity) context,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        }
    }

    private void setupListeners() {
        uploadButton.setOnClickListener(v -> {
            if (selectedImageBitmap != null) {
                uploadImageToDatabase(selectedImageBitmap);
            } else {
                Log.e("UploadImage", "No image selected!");
            }
        });

        cameraButton.setOnClickListener(v -> launchCamera());
        galleryButton.setOnClickListener(v -> launchGallery());

        repairButton.setOnClickListener(v -> {
            if (selectedImage.getDrawable() != null) {
                markAsRepaired();
                selectedImage.setImageDrawable(null);
            } else {
                showToast("Please upload a picture");
            }
        });

        cancelButton.setOnClickListener(v -> hideCard());
    }

    public void showCard(Marker marker) {
        currentMarker = marker;
        LatDetails.setText(String.valueOf(marker.getPosition().latitude));
        LngDetails.setText(String.valueOf(marker.getPosition().longitude));
        markerCard.setVisibility(View.VISIBLE);
        markerCard.bringToFront();
        markerCard.setElevation(50);
        markerCard.requestLayout();
    }

    public void hideCard() {
        markerCard.setVisibility(View.GONE);
        selectedImage.setImageDrawable(null);
    }

    private void launchCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(context.getPackageManager()) != null) {
            ((Activity) context).startActivityForResult(takePictureIntent, REQUEST_CAMERA);
        } else {
            showToast("No camera app available");
        }
    }

    private void launchGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            ((Activity) context).startActivityForResult(intent, REQUEST_GALLERY);
        } else {
            showToast("No gallery app available");
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CAMERA && data != null && data.getExtras() != null) {
                Bitmap imageBitmap = (Bitmap) data.getExtras().get("data");
                handleImageCapture(imageBitmap);
            } else if (requestCode == REQUEST_GALLERY && data != null) {
                imageUri = data.getData();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), imageUri);
                    selectedImage.setImageBitmap(bitmap);
                    selectedImageBitmap = bitmap;  // Set the global variable
                    markerImageMap.put(getMarkerKey(currentMarker), imageUri);
                    Log.d("GalleryResult", "Image selected from gallery");
                } catch (IOException e) {
                    Log.e("GalleryResult", "Error getting image from gallery: " + e.getMessage());
                }
            }
        }
    }
    private Uri compressImageAndStoreLocally(Bitmap imageBitmap) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] compressedImage = baos.toByteArray();

            File imageFile = new File(context.getCacheDir(), UUID.randomUUID().toString() + ".jpg");
            FileOutputStream fos = new FileOutputStream(imageFile);
            fos.write(compressedImage);
            fos.flush();
            fos.close();

            Log.d("CompressImage", "Image compressed and stored locally");
            return Uri.fromFile(imageFile);

        } catch (IOException e) {
            e.printStackTrace();
            Log.e("CompressImage", "Error compressing image: " + e.getMessage());
            return null;
        }
    }

    private void handleImageCapture(Bitmap imageBitmap) {
        if (imageBitmap != null) {
            selectedImage.setImageBitmap(imageBitmap);
            selectedImageBitmap = imageBitmap;  // Set the global variable
            imageUri = compressImageAndStoreLocally(imageBitmap);
            markerImageMap.put(getMarkerKey(currentMarker), imageUri);
            Log.d("CameraResult", "Image captured and stored locally");
        } else {
            Log.d("CameraResult", "Captured image bitmap is null");
        }
    }


    private void uploadImageToDatabase(Bitmap imageBitmap) {
        String base64Image = encodeImageToBase64(imageBitmap);

        // Make sure the currentMarker is not null
        if (currentMarker == null) {
            Log.e("UploadImage", "No marker selected for upload");
            return;
        }

        double markerLat = currentMarker.getPosition().latitude;
        double markerLng = currentMarker.getPosition().longitude;
        String markerDescription = "Repaired marker"; // Add your description here

        // Create a reference to the Firebase database
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReferenceFromUrl("https://test-f6ef2-default-rtdb.firebaseio.com/repairedmarkers");

        // Prepare your marker data with the Base64 encoded image
        Map<String, Object> markerData = new HashMap<>();
        markerData.put("image", base64Image);  // Base64 string of the image
        markerData.put("lat", markerLat);
        markerData.put("lng", markerLng);
        markerData.put("description", markerDescription);

        // Push data to the database
        databaseRef.push().setValue(markerData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("UploadImage", "Image uploaded successfully!");
                    } else {
                        Log.e("UploadImage", "Error uploading image: ", task.getException());
                    }
                });
    }

    // Method to convert image to Base64
    private String encodeImageToBase64(Bitmap imageBitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);  // Convert image to byte stream
        byte[] imageBytes = baos.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);  // Encode to Base64
    }
    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
    private void markAsRepaired() {
        if (currentMarker != null) {
            double latitude = currentMarker.getPosition().latitude;
            double longitude = currentMarker.getPosition().longitude;

            // Get the current timestamp
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

            // Add the log to the local database
            databaseHelper.addRepairLog(latitude, longitude, timestamp);
            removeMarkerFromDatabase(latitude, longitude);

            // Remove the marker from the map
            currentMarker.remove();
        }
        hideCard();
        // logic to remove from database that is used to get markers
    }
    public void removeMarkerFromDatabase(final double selectedMarkerLat, final double selectedMarkerLng) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("users");

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean markerFound = false;

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Double dbLongitude = snapshot.child("longitude").getValue(Double.class);
                    Double dbLatitude = snapshot.child("latitude").getValue(Double.class);

                    // Log raw values from database
                    Log.d("RemoveMarker", "DB Original Latitude: " + dbLatitude + ", DB Original Longitude: " + dbLongitude);

                    // Rounded values for consistent comparison
                    double roundedSelectedLat = Math.round(selectedMarkerLat * 1000000.0) / 1000000.0;
                    double roundedSelectedLng = Math.round(selectedMarkerLng * 1000000.0) / 1000000.0;
                    double roundedDbLat = Math.round(dbLatitude * 1000000.0) / 1000000.0;
                    double roundedDbLng = Math.round(dbLongitude * 1000000.0) / 1000000.0;

                    // Log rounded values
                    Log.d("RemoveMarker", "Rounded Selected Latitude: " + roundedSelectedLat + ", Rounded Selected Longitude: " + roundedSelectedLng);
                    Log.d("RemoveMarker", "DB Rounded Latitude: " + roundedDbLat + ", DB Rounded Longitude: " + roundedDbLng);

                    // Precision margin for comparison
                    double margin = 0.0001;  // Adjusted margin for better tolerance

                    // Check if the rounded values match
                    if (Math.abs(roundedDbLat - roundedSelectedLat) < margin && Math.abs(roundedDbLng - roundedSelectedLng) < margin) {
                        // Marker found
                        markerFound = true;

                        // Remove the marker from the database
                        snapshot.getRef().removeValue().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.d("RemoveMarker", "Marker successfully removed from database");
                            } else {
                                Log.e("RemoveMarker", "Error removing marker: " + task.getException().getMessage());
                            }
                        });
                        break;
                    } else {
                        // Log the absolute differences for further investigation
                        Log.d("RemoveMarker", "No match: DB Lat: " + dbLatitude + " Marker Lat: " + selectedMarkerLat);
                        Log.d("RemoveMarker", "No match: DB Lng: " + dbLongitude + " Marker Lng: " + selectedMarkerLng);
                        Log.d("RemoveMarker", "Latitude Difference: " + Math.abs(dbLatitude - selectedMarkerLat));
                        Log.d("RemoveMarker", "Longitude Difference: " + Math.abs(dbLongitude - selectedMarkerLng));
                    }
                }

                if (!markerFound) {
                    Log.d("RemoveMarker", "No matching marker found in database.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("RemoveMarker", "Database error: " + databaseError.getMessage());
            }
        });
    }
    private String getMarkerKey(Marker marker) {
        return marker.getPosition().latitude + "_" + marker.getPosition().longitude;
    }
}
