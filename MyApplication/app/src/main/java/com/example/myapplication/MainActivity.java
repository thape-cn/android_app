package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity{
    private BroadcastReceiver messageReceiver;


    private MySocketThread myThread;

    private static class MySocketThread extends Thread{

        private String url = null;
        private String receiveId = null;
        private String content = null;

//        private Socket socket;
        private HttpURLConnection connection;

        public MySocketThread(String url, String receiveId, String content) {
            this.url = url;
            this.receiveId = receiveId;
            this.content = content;
        }

        @Override
        public void run(){
            try {
//                this.socket = new Socket("127.0.0.1", 10000);       //change to you own server IP address
//                this.socket.setSoTimeout(10000);
//                OutputStream os = this.socket.getOutputStream();
//                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os, "utf-8"));
//                bw.write(value);
//                bw.flush();
//                this.socket.close();
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
                body.put("content", this.content);
                bw.write(body.toString());
                bw.close();
                int responseCode = this.connection.getResponseCode();
//                if(responseCode == HttpURLConnection.HTTP_OK){
                    InputStream inputStream = this.connection.getInputStream();
//                    String result = is2String(inputStream);//将流转换为字符串。
                    Log.d("karl","result=============" + inputStream.toString());
//                }
            } catch (IOException | JSONException e){
                e.printStackTrace();
            } finally {
//                this.socket.close();
                this.connection.disconnect();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final SharedPreferences sharedPreferences = this.getSharedPreferences("settings", Context.MODE_PRIVATE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final EditText phone = (EditText) findViewById(R.id.phone);
        final EditText serverUrl = (EditText) findViewById(R.id.serverUrl);
        phone.setText(sharedPreferences.getString("phone", ""));
        serverUrl.setText(sharedPreferences.getString("serverUrl", ""));
        Switch listenOnOff = (Switch) findViewById(R.id.listenOnOff);
        listenOnOff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    phone.setFocusable(false);
                    serverUrl.setFocusable(false);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("phone", phone.getText().toString());
                    editor.putString("serverUrl", serverUrl.getText().toString());
                    editor.apply();
                    //Toast.makeText(MainActivity.this, "applying permission", Toast.LENGTH_LONG).show();

                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
                        //Toast.makeText(MainActivity.this, "Permission must be granted to get sms", Toast.LENGTH_LONG).show();
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECEIVE_SMS}, 1);
                        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
                            //Toast.makeText(MainActivity.this, "Permission must be granted to get sms", Toast.LENGTH_LONG).show();
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_SMS}, 1);
                        }
                    }
                    messageReceiver = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            String url = sharedPreferences.getString("serverUrl", "");
                            String receiveId = sharedPreferences.getString("phone", "");
                            Bundle bundle = intent.getExtras();
                            String value = bundle.getString("name");
                            //Toast.makeText(context, value, Toast.LENGTH_LONG).show();
                            //Intent intentTmp = new Intent(MainActivity.this, SocketActivity.class);
                            //intentTmp.putExtra("extra_data", value);
                            //startActivity(intentTmp);
                            myThread = new MySocketThread(url, receiveId, value);
                            myThread.start();
                        }
                    };
                    registerReceiver(messageReceiver, new IntentFilter("CLOSE_ACTION"));
                    Toast.makeText(MainActivity.this, "功能开启", Toast.LENGTH_LONG).show();
                }
                else{
                    unregisterReceiver(messageReceiver);
                    //Do something
                    phone.setFocusableInTouchMode(true);
                    serverUrl.setFocusableInTouchMode(true);
                    Toast.makeText(MainActivity.this, "功能关闭", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
