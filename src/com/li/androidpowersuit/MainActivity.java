package com.li.androidpowersuit;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryStats;
import android.os.Bundle;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.android.internal.app.IBatteryStats;
import com.android.internal.os.BatteryStatsImpl;
import com.android.internal.os.PowerProfile;
import com.example.androidpowersuit.R;
import com.li.power.PowerUsage;
import com.li.service.ModuleStateService;

public class MainActivity extends Activity {
	Thread td=null;
	 private int mStatsType = BatteryStats.STATS_SINCE_CHARGED;
	 BatteryStats mStats;
	 PowerUsage powerUsage;
	PowerProfile mPowerProfile ;//= new PowerProfile(MainActivity.this);
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState); 
		setContentView(R.layout.activity_main);
		Button bnStart=(Button)findViewById(R.id.button_start);
		Button bnStop=(Button)findViewById(R.id.button_stop);
		mPowerProfile = new PowerProfile(MainActivity.this);
		bnStart.setOnClickListener(new OnClickListener(){
			public void onClick(View v){
				ModuleStateService.condition=true;
				Intent intent =new Intent(MainActivity.this,ModuleStateService.class);
				startService(intent);
				System.out.println("service Begin.....");
			}
		});
		
		bnStop.setOnClickListener(new OnClickListener(){
			public void onClick(View v){
				ModuleStateService.condition=false;
				Intent intent =new Intent(MainActivity.this,ModuleStateService.class);
				stopService(intent);
				System.out.println("service Stop.....");
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}


}
