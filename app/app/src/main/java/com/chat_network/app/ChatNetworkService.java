package com.chat_network.app;


import java.util.List;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface ChatNetworkService {
    @FormUrlEncoded
    @POST("getChatHistory")
    Call<List<String>> getChatHistory(@Field("channelName") String channelName);

    @FormUrlEncoded
    @POST("addMsg")
    Call<List<String>> addMst(@Field("channelName") String channelName, @Field("content") String content);
}
