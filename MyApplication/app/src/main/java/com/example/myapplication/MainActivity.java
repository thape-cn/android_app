package com.example.myapplication;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private BroadcastReceiver conReceiver;
    private BroadcastReceiver messageReceiver;
    private final String[] phoneNumbers = {"", ""};

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
                String url = serverUrlStr + "?receiveIds=" + receiveIds;
                conReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (intent.getAction().equals(Intent.ACTION_TIME_TICK)) {
                            BatteryManager batterymanager = (BatteryManager) getSystemService(BATTERY_SERVICE);
                            int battery = batterymanager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                            boolean isCharging = batterymanager.isCharging();
                            MyConThread conThread = new MyConThread(url + "&battery=" + battery + "&is_charging=" + isCharging);
                            conThread.start();
                        }
                    }
                };
                registerReceiver(conReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));

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
                        Log.d("flag", String.valueOf(flag));
                        if (flag) {
                            MySocketThread myThread = new MySocketThread(url, receiveId, sendId, content);
                            myThread.start();
                        }
                    }
                };
                registerReceiver(messageReceiver, new IntentFilter("CLOSE_ACTION"));
                Toast.makeText(MainActivity.this, "功能开启", Toast.LENGTH_LONG).show();
            } else {
                unregisterReceiver(conReceiver);
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
