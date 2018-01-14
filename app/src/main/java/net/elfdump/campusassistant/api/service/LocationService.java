package net.elfdump.campusassistant.api.service;

import net.elfdump.campusassistant.api.model.UserPlaceEvent;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface LocationService {

    @POST("location/track/room")
    Call<Void> roomChange(@Body UserPlaceEvent placeEvent);

    @POST("location/track/room/get")
    Call<Map<String, Integer>> getPeopleCount();

}
