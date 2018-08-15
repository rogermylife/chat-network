package com.chat_network.app;


import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface ChatNetworkService {
    @FormUrlEncoded
    @POST("getChatHistory")
    Call<ResponseBody> getChatHistory(@Field("channelName") String channelName);

    @FormUrlEncoded
    @POST("addMsg")
    Call<ResponseBody> addMsg(@Field("channelName") String channelName, @Field("content") String content);
}
