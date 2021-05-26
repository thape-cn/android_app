package com.example.myapplication;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MySocketThread extends Thread {
    private final String url;
    private final String receiveId;
    private final String sendId;
    private final String content;

    // private Socket socket;
    private HttpURLConnection connection;

    public MySocketThread(String url, String receiveId, String sendId, String content) {
        this.url = url;
        this.receiveId = receiveId;
        this.sendId = sendId;
        this.content = content;
    }

    @Override
    public void run() {
        try {
            URL url = new URL(this.url);
            this.connection = (HttpURLConnection) url.openConnection();
            //设置请求方法
            this.connection.setRequestMethod("POST");
            this.connection.setDoOutput(true);
            this.connection.setDoInput(true);
            this.connection.setUseCaches(false);
            this.connection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
            this.connection.connect();
            OutputStream os = this.connection.getOutputStream();
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
            JSONObject body = new JSONObject();
            body.put("receiveId", this.receiveId);
            body.put("sendId", this.sendId);
            body.put("content", this.content);
            bw.write(body.toString());
            bw.close();
            int responseCode = this.connection.getResponseCode();
            Log.d("code", "result===" + responseCode);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        } finally {
            this.connection.disconnect();
        }
    }
}
