package com.li.helpclass;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

public class WifiFlow {

	private final String TAG = "wifi";
	// wifi上传流量速度
	public long wifiup;
	// wifi下载流量速度
	public long wifidown;

	//wifi上传流量速度
	public long getwifiup() {
		return wifiup;
	}
	//wifi下载流量速度
	public long getwifidown() {
		return wifidown;
	}
	
	
	public WifiFlow(Context context) {
		
		// 流量
		final Handler handler = new Handler() {
			long gjtx1=TrafficMonitoring.wReceive();
			long formerwifi=TrafficMonitoring.wReceive();

			long gjtu1=TrafficMonitoring.wSend();
			long formerwifi2=TrafficMonitoring.wSend();

			public void handleMessage(Message msg) {
				formerwifi = gjtx1;
				gjtx1 = TrafficMonitoring.wReceive();
				if(gjtx1<formerwifi){
					formerwifi = gjtx1;
				}
				wifiup = gjtx1 - formerwifi;
				
				//Log.i("up", wifiup + "");

				formerwifi2 = gjtu1;
				gjtu1 = TrafficMonitoring.wSend();
				if(gjtu1<formerwifi2){
					formerwifi2 = gjtu1;
				}
				wifidown = gjtu1 - formerwifi2;
				//Log.i("down", wifidown + "");
				
				
				super.handleMessage(msg);
			}
		};

		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				Message message = new Message();
				message.what = 1;
				handler.sendMessage(message);
			}
		};
		Timer timer = new Timer();
		timer.schedule(task, 100, 1000);

	}

}