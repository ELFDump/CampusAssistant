package net.elfdump.campusassistant.api.model;

public class UserEvent extends Event{
    private String userId;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userUuid) {
        this.userId = userUuid;
    }
}
