package net.elfdump.campusassistant.api.model;

public class UserEvent extends Event {
    private String uuid;

    public String getUserId() {
        return uuid;
    }

    public void setUserId(String userUuid) {
        this.uuid = userUuid;
    }
}
