package com.example.myapplication;

import android.Manifest;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private BroadcastReceiver messageReceiver;
    private final String[] phoneNumbers = {"", ""};

    private MySocketThread myThread;

    private TimeCountDown timeCountDown;

    private boolean isWorked(String className) {
        ActivityManager myManager = (ActivityManager) MainActivity.this.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        ArrayList<ActivityManager.RunningServiceInfo> runningService = (ArrayList<ActivityManager.RunningServiceInfo>) myManager.getRunningServices(30);
        for (int i = 0; i < runningService.size(); i++) {
            if (runningService.get(i).service.getClassName().equals(className)) {
                return true;
            }
        }
        return false;
    }

    private static class MySocketThread extends Thread {

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
                body.put("sendId", this.sendId);
                body.put("content", this.content);
                bw.write(body.toString());
                bw.close();
                int responseCode = this.connection.getResponseCode();
                Log.d("code", "result===" + responseCode);
//                if(responseCode == HttpURLConnection.HTTP_OK){
//                InputStream inputStream = this.connection.getInputStream();
//                    String result = is2String(inputStream);//将流转换为字符串。
//                Log.d("karl", "result=============" + inputStream.toString());
//                }
            } catch (IOException | JSONException e) {
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
        //Toast.makeText(MainActivity.this, "applying permission", Toast.LENGTH_LONG).show();
        if (SimUtil.isHasPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE)) {
            SimUtil.requestOnePermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE, 1);
        }
        if (SimUtil.isHasPermission(MainActivity.this, Manifest.permission.RECEIVE_SMS)) {
            SimUtil.requestOnePermission(MainActivity.this, Manifest.permission.RECEIVE_SMS, 1);
            if (SimUtil.isHasPermission(MainActivity.this, Manifest.permission.READ_SMS)) {
                SimUtil.requestOnePermission(MainActivity.this, Manifest.permission.READ_SMS, 1);
            }
        }
        boolean isDoubleSim = SimUtil.isDoubleSim(MainActivity.this);
        phoneNumbers[0] = SimUtil.getSlotIdInfo(MainActivity.this, 0).number;
        if (isDoubleSim) {
            phoneNumbers[1] = SimUtil.getSlotIdInfo(MainActivity.this, 1).number;
        } else {
            if (TextUtils.isEmpty(phoneNumbers[0])) {
                phoneNumbers[0] = SimUtil.getSlotIdInfo(MainActivity.this, 1).number;
            }
        }
        final SharedPreferences sharedPreferences = this.getSharedPreferences("settings", Context.MODE_PRIVATE);
        final EditText matchKeys = findViewById(R.id.matchKeys);
        final EditText phone = findViewById(R.id.phone);
        final EditText subPhone = findViewById(R.id.subPhone);
        final EditText serverUrl = findViewById(R.id.serverUrl);
        Switch listenOnOff = findViewById(R.id.listenOnOff);
        matchKeys.setText(sharedPreferences.getString("matchKeys", ""));
        String phoneText = sharedPreferences.getString("phone", phoneNumbers[0]);
        if (!TextUtils.isEmpty(phoneText)) phone.setText(phoneText);
        String subPhoneText = sharedPreferences.getString("subPhone", phoneNumbers[1]);
        if (!TextUtils.isEmpty(subPhoneText)) subPhone.setText(sharedPreferences.getString("subPhone", phoneNumbers[1]));
        serverUrl.setText(sharedPreferences.getString("serverUrl", "https://cybros.thape.com.cn/api/received_sms_message.json"));
        listenOnOff.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                matchKeys.setFocusable(false);
                phone.setFocusable(false);
                subPhone.setFocusable(false);
                serverUrl.setFocusable(false);
                String phoneStr = phone.getText().toString();
                String subPhoneStr = subPhone.getText().toString();
                String serverUrlStr = serverUrl.getText().toString();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("matchKeys", matchKeys.getText().toString());
                editor.putString("phone", phoneStr);
                editor.putString("subPhone", subPhoneStr);
                editor.putString("serverUrl", serverUrlStr);
                editor.apply();
                ArrayList<String> receiveIdList = new ArrayList<>();
                if (!TextUtils.isEmpty(phoneStr)) {
                    receiveIdList.add(phoneStr);
                }
                if (!TextUtils.isEmpty(subPhoneStr)) {
                    receiveIdList.add(subPhoneStr);
                }
                String receiveIds = TextUtils.join(",", receiveIdList);
                timeCountDown = new TimeCountDown(1000 * 60 * 60, 1000 * 60 * 5, serverUrlStr + "?receiveIds=" + receiveIds);
                timeCountDown.start();
                messageReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String keys = sharedPreferences.getString("matchKeys", "");
                        String url = sharedPreferences.getString("serverUrl", "");
                        Bundle bundle = intent.getExtras();
                        String sendId = bundle.getString("sendId");
                        String content = bundle.getString("content");
                        int slotId = bundle.getInt("slotId");
                        String receiveId = bundle.getString("receiveId");
                        if (TextUtils.isEmpty(receiveId)) {
                            String receiveKey = slotId == 0 ? "phone" : "subPhone";
                            receiveId = sharedPreferences.getString(receiveKey, "");
                        }
                        boolean flag = keys.equals("");
                        if (!flag) {
                            String[] temp = keys.split(",");
                            for (String s : temp) {
                                flag = content.contains(s);
                                if (flag) break;
                            }
                        }
                        //Toast.makeText(context, value, Toast.LENGTH_LONG).show();
                        //Intent intentTmp = new Intent(MainActivity.this, SocketActivity.class);
                        //intentTmp.putExtra("extra_data", value);
                        //startActivity(intentTmp);
                        Log.d("flag", String.valueOf(flag));
                        if (flag) {
                            myThread = new MySocketThread(url, receiveId, sendId, content);
                            myThread.start();
                        }
                    }
                };
                registerReceiver(messageReceiver, new IntentFilter("CLOSE_ACTION"));
                Toast.makeText(MainActivity.this, "功能开启", Toast.LENGTH_LONG).show();
            } else {
                timeCountDown.cancel();
                unregisterReceiver(messageReceiver);
                //Do something
                matchKeys.setFocusableInTouchMode(true);
                phone.setFocusableInTouchMode(true);
                subPhone.setFocusableInTouchMode(true);
                serverUrl.setFocusableInTouchMode(true);
                Toast.makeText(MainActivity.this, "功能关闭", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
      if(keyCode==KeyEvent.KEYCODE_BACK){
        moveTaskToBack(false);
        return true;
      }
      return super.onKeyDown(keyCode, event);
    }
}
