package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.os.Bundle;
import android.telephony.SmsMessage;

import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MyReceiver extends BroadcastReceiver {
    private static final String SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        //判断广播消息
        if (action.equals(SMS_RECEIVED_ACTION)) {
            Bundle bundle = intent.getExtras();
            //如果不为空
            if (bundle != null) {
                //将pdus里面的内容转化成Object[]数组
                Object[] pdusData = (Object[]) bundle.get("pdus"); // pdus ：protocol data unit  ：
                int subId = bundle.getInt("subscription");
                int slotId = SimUtil.getSlot(context, String.valueOf(subId));
                SlotInfo slotInfo = SimUtil.getSlotIdInfo(context, slotId);
                //解析短信
                String format = intent.getStringExtra("format");
                SmsMessage[] msg = new SmsMessage[pdusData.length];
                for (int i = 0; i < msg.length; i++) {
                    byte[] pdus = (byte[]) pdusData[i];
                    msg[i] = SmsMessage.createFromPdu(pdus, format);
                }
                StringBuilder content = new StringBuilder(); //获取短信内容
                StringBuilder phoneNumber = new StringBuilder(); //获取地址
                //分析短信具体参数
                for (SmsMessage temp : msg) {
                    content.append(temp.getMessageBody());
                    phoneNumber.append(temp.getOriginatingAddress());
                }
                //Toast.makeText(context, "发送者号码：" + phoneNumber.toString() + "  短信内容：" + content.toString(), Toast.LENGTH_LONG).show();
                //Intent intent1 = new Intent(context, MainActivity.class);//
                Intent i = new Intent("CLOSE_ACTION");
                Bundle bundle1 = new Bundle();
                bundle1.putString("sendId", phoneNumber.toString());
                bundle1.putString("content", content.toString());
                bundle1.putInt("subId", subId);
                bundle1.putInt("slotId", slotId);
                bundle1.putString("carrierName", slotInfo.carrierName);
                bundle1.putString("receiveId", slotInfo.number);
                i.putExtras(bundle1);
                context.sendBroadcast(i);
            }
        }
    }

    public void sendToServer(String content) {

    }

    public String smsToString(Context context, String sendId, String content) {
        String res = "";
        JSONObject object = new JSONObject();
        try {
            JSONArray jsonArray = new JSONArray();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("sendId", sendId);
            jsonObject.put("content", content);
            jsonArray.put(jsonObject);
            object.put("sms", jsonArray);
            res = object.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Toast.makeText(context, res, Toast.LENGTH_LONG).show();
        return res;
    }
}

