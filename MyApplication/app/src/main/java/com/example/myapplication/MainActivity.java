package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity{

    private EditText phone;
    private EditText serverUrl;
    private Switch listenOnOff;
    private IntentFilter intentFilter;
    private BroadcastReceiver messageReceiver;

    private MyReceiver myReceiver;

    public String value = null;
    public String message;

    private MySocketThread myThread;

    private class MySocketThread extends Thread{

//        private Socket socket;
        private HttpURLConnection connection;

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
                String myPhone = phone.getText().toString();
                URL url = new URL(serverUrl.getText().toString());
                this.connection = (HttpURLConnection) url.openConnection();
                //设置请求方法
                this.connection.setRequestMethod("POST");
                this.connection.setDoOutput(true);
                this.connection.setDoInput(true);
                this.connection.setUseCaches(false);
                this.connection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
                this.connection.connect();
                OutputStream os = this.connection.getOutputStream();
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os, "utf-8"));
                JSONObject body = new JSONObject();
                body.put("receiveId", myPhone);
                body.put("content", value);
                bw.write(body.toString());
                bw.close();
                int responseCode = this.connection.getResponseCode();
                Log.d("kwwl", String.valueOf(responseCode));
                if(responseCode == HttpURLConnection.HTTP_OK){
//                    InputStream inputStream = connection.getInputStream();
//                    String result = is2String(inputStream);//将流转换为字符串。
//                    Log.d("kwwl","result============="+result);
                }
            }
            catch (UnknownHostException e){
                e.printStackTrace();
            }
            catch (IOException e){
                e.printStackTrace();
            }
            catch (JSONException e){
                e.printStackTrace();
            } finally {
//                this.socket.close();
                this.connection.disconnect();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        phone = (EditText) findViewById(R.id.phone);
        serverUrl = (EditText) findViewById(R.id.serverUrl);
        listenOnOff = (Switch) findViewById(R.id.listenOnOff);
        listenOnOff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked == true) {
                    phone.setFocusable(false);
                    serverUrl.setFocusable(false);
                    //Toast.makeText(MainActivity.this, "applying permission", Toast.LENGTH_LONG).show();

                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
                        //Toast.makeText(MainActivity.this, "Permission must be granted to get sms", Toast.LENGTH_LONG).show();
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECEIVE_SMS}, 1);
                        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
                            //Toast.makeText(MainActivity.this, "Permission must be granted to get sms", Toast.LENGTH_LONG).show();
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_SMS}, 1);
                        }
                    } else {
                        //Toast.makeText(MainActivity.this, "permission granted", Toast.LENGTH_LONG).show();
                        //intentFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
                        //myReceiver = new MyReceiver();
                        //registerReceiver(myReceiver, intentFilter);
                        messageReceiver = new BroadcastReceiver() {
                            @Override
                            public void onReceive(Context context, Intent intent) {
                                Bundle bundle = intent.getExtras();
                                value = bundle.getString("name");
                                //Toast.makeText(context, value, Toast.LENGTH_LONG).show();
                                //Intent intentTmp = new Intent(MainActivity.this, SocketActivity.class);
                                //intentTmp.putExtra("extra_data", value);
                                //startActivity(intentTmp);
                                myThread = new MySocketThread();
                                myThread.start();
                            }
                        };
                        registerReceiver(messageReceiver, new IntentFilter("CLOSE_ACTION"));

                    }
                    Toast.makeText(MainActivity.this, "I'm here", Toast.LENGTH_LONG).show();
                    if (value != null) {

                    }
                }
                else{
                    //Do something
                    phone.setFocusableInTouchMode(true);
                    serverUrl.setFocusableInTouchMode(true);
                }
            }
        });
    }
}
