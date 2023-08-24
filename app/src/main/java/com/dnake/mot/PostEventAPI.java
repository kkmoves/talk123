package com.dnake.mot;

import android.util.Log;

import org.json.JSONObject;
import org.json.JSONArray;

import java.util.concurrent.TimeUnit;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class PostEventAPI {
    public static void doPost(JSONObject jsonObject, String API) {
        try {
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build();

            String format = "application/JSON; charset=utf-8";
            RequestBody requestBody = RequestBody.create(MediaType.parse(format), jsonObject.toString().replace("\\/","/").replace("\\n",""));
            Request request = new okhttp3.Request.Builder()
                    .addHeader("Content-Type", format)
                    .addHeader("Accept", format)
                    .url(API)
                    .post(requestBody).build();
            okhttp3.Response response = okHttpClient.newCall(request).execute();
            if (response.code() == 200) {
                String resString = response.body().string();
                Log.i("Success!", "Already Send Event "+jsonObject.get("EventType"));
            }
        } catch (Exception e) {
            Log.d("post error",""+e);
        }
    }
}
