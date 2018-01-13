package net.elfdump.campusassistant;

class MapViewConfig extends MapViewConfig {

    public MyMapViewConfig(@NotNull Context applicationContext) {
        super(applicationContext);
    }

    // override attributes, getters, methods etc.

    @Override
    public int getRoomBackgroundColor(@NotNull IndoorwayObjectParameters obj) {
        return getApplicationContext().getResources().getColor(R.color.colorAccent);
    }

    @Override
    public int getRoomOutlineColor(@NotNull IndoorwayObjectParameters obj) {
        return getApplicationContext().getResources().getColor(R.color.colorPrimary);
    }

}
