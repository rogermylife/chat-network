package com.chat_network.app;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ServiceController implements Callback<ResponseBody> {

        static final String BASE_URL = "http://10.0.2.2:10080/myapp/";

        public void start() {


            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            ChatNetworkService service = retrofit.create(ChatNetworkService.class);

            //get msg
            Call<ResponseBody> call = service.getChatHistory("officialchannel");
            call.enqueue(this);

            //send msg
            call = service.addMsg("officialchannel","test123");
            call.enqueue(this);



        }

        @Override
        public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
            if(response.isSuccessful()) {
                System.out.println("YESSSSSSS");
                ResponseBody rb = response.body();
                try {
                    System.out.println(rb.string());
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                changesList.forEach(res -> System.out.println(res));
            } else {
                System.out.println(response.errorBody());
            }
        }

        @Override
        public void onFailure(Call<ResponseBody> call, Throwable t) {
            System.out.println("HOOOOOOOOOOOOO");
            t.printStackTrace();
        }
}
