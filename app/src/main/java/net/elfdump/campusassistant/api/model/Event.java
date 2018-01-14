package net.elfdump.campusassistant.api.model;

public class Event {
    private float time;

    public float getTimeInSeconds() {
        return time;
    }

    public void setTimeInSeconds(float time) {
        this.time = time;
    }

    public long getTimeInMilliseconds() {
        return (long) (time * 1000);
    }

    public void setTimeInMilliseconds(long time) {
        this.time = ((float) time) / 1000;
    }
}
