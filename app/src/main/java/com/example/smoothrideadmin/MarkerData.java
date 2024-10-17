package com.example.smoothrideadmin;

public class MarkerData {
    private String id;
    private double latitude;
    private double longitude;
    private String imageUrl;

    public MarkerData() {
        // Default constructor required for calls to DataSnapshot.getValue(MarkerData.class)
    }

    public MarkerData(String id, double latitude, double longitude, String imageUrl) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.imageUrl = imageUrl;
    }

    public String getId() {
        return id;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
