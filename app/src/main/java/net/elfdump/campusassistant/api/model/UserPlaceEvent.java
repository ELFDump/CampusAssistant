package net.elfdump.campusassistant.api.model;

public class UserPlaceEvent extends UserEvent{
    public enum PlaceAction {
        LEAVE, ENTER
    }

    private String placeId;
    private PlaceAction action;

    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    public PlaceAction getAction() {
        return action;
    }

    public void setAction(PlaceAction action) {
        this.action = action;
    }
}