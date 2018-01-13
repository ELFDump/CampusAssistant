package net.elfdump.campusassistant.api.model;

@Deprecated // Zrobi się to przez Indoorway jeśli już
public class UserLocationEvent extends UserEvent{
    private double latitude;
    private double longitude;

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
