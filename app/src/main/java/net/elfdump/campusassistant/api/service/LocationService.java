package net.elfdump.campusassistant.api.service;

import net.elfdump.campusassistant.api.model.UserLocationEvent;
import net.elfdump.campusassistant.api.model.UserPlaceEvent;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface LocationService {

    @POST("location/track/room")
    Call<UserPlaceEvent> roomChange(@Body UserPlaceEvent placeEvent);


    @POST("location/track")
    Call<UserLocationEvent> update(@Body UserLocationEvent placeEvent);

}
