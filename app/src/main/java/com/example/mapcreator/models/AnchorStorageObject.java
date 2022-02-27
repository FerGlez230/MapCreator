package com.example.mapcreator.models;

public class AnchorStorageObject {
    private String shortCode;
    private String idAnchor;
    private double latitude;
    private double longitude;
    public AnchorStorageObject() {
    }

    public AnchorStorageObject(String shortCode, String idAnchor, double latitude, double longitude) {

        this.idAnchor = idAnchor;
        this.shortCode = shortCode;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() { return latitude; }

    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }

    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getShortCode() {
        return shortCode;
    }
    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public String getIdAnchor() {
        return idAnchor;
    }

    public void setIdAnchor(String idAnchor) {
        this.idAnchor = idAnchor;
    }


}
