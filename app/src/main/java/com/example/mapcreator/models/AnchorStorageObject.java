package com.example.mapcreator.models;

public class AnchorStorageObject {
    private int shortCode;
    private String idAnchor;
    private String latitude;
    private String longitude;
    private String description;
    public AnchorStorageObject() {
    }

    public int getShortCode() {
        return shortCode;
    }
    public void setShortCode(int shortCode) {
        this.shortCode = shortCode;
    }

    public String getIdAnchor() {
        return idAnchor;
    }

    public void setIdAnchor(String idAnchor) {
        this.idAnchor = idAnchor;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String lat) {
        this.latitude = lat;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitud) {
        this.longitude = longitud;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
