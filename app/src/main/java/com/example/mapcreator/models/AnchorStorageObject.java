package com.example.mapcreator.models;

public class AnchorStorageObject {
    private int shortCode;
    private String idAnchor;
    private String lat;
    private String longitud;
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

    public String getLat() {
        return lat;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getLongitud() {
        return longitud;
    }

    public void setLongitud(String longitud) {
        this.longitud = longitud;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
