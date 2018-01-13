package net.elfdump.campusassistant.api;

import net.elfdump.campusassistant.api.service.LocationService;
import net.elfdump.campusassistant.api.service.UserService;

import retrofit2.Retrofit;

public class RestClient {

    private static final String baseUrl = "http://192.168.100.79/campus-assistant-api/public_html";
    private Retrofit retrofit;

    public RestClient() {
        retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .build();
    }

    public LocationService location() {
        return retrofit.create(LocationService.class);
    }

    public UserService users() {
        return retrofit.create(UserService.class);
    }

}
