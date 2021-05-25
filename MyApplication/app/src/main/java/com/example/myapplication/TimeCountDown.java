package com.example.myapplication;

import android.os.CountDownTimer;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

class MyConThread extends Thread {
    private final String url;

    public MyConThread(String url) {
        this.url = url;
    }

    @Override
    public void run() {
        try {
            URL url = new URL(this.url);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            //设置请求方法
            connection.setRequestMethod("GET");
            connection.connect();
            //得到响应码
            int responseCode = connection.getResponseCode();
            Log.d("conUrl", this.url);
            Log.d("conHttpCode",String.valueOf(responseCode));
			connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


public class TimeCountDown extends CountDownTimer {
	private final String url;

	public TimeCountDown(long millisInFuture, long countDownInterval, String url) {
		super(millisInFuture, countDownInterval);
		// TODO Auto-generated constructor stub
		this.url = url;
	}

	@Override
	public void onTick(long millisUntilFinished) {
		// TODO Auto-generated method stub
        MyConThread thread = new MyConThread(this.url);
        thread.start();
	}

	@Override
	public void onFinish() {
		// TODO Auto-generated method stub
        this.start();
	}
}
