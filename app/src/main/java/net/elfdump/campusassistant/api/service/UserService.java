package net.elfdump.campusassistant.api.service;

import net.elfdump.campusassistant.api.model.User;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

@Deprecated // NIE ROBIĆ TYCH USERÓW!!!!!
public interface UserService {

    @POST("users/create")
    Call<User> create(@Body User user);

    @GET("users/{uuid}")
    Call<User> get(@Path("uuid") String uuid);

}
