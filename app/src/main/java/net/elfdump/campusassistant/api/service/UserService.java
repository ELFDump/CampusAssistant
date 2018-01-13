package net.elfdump.campusassistant.api.service;

import net.elfdump.campusassistant.api.model.User;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface UserService {

    @POST("users/create")
    Call<User> create(@Body User user);

}
