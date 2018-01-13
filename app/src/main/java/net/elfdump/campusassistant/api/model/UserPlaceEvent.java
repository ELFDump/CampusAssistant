package net.elfdump.campusassistant.api.model;

public class UserPlaceEvent extends UserLocationEvent{
    private String placeId;
    private PlaceAction action;
}

enum PlaceAction {
    LEAVE, ENTER
}