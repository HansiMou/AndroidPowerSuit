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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

public class ThirdGeneration {

	private final String TAG = "G3Example";
	// private TextView mLabel3G;
	// 信号强度
	public int GSM;
	public int WCDMA;
	public int EVO;
	//信号类型
	public int networktype;
	// 3g上传流量速度
	public long g3up;
	// 3g下载流量速度
	public long g3down;
	
	// gps状态
	public boolean isGpsEnable;
	public int gpsNum;
	TelephonyManager tel;
	List<GpsSatellite> numSatelliteList = new ArrayList<GpsSatellite>(); // 卫星信号
	LocationManager locationManager;

	

	//联通2G还是3G
	public boolean is3G(){
		if(networktype == 2)
			return false;
		else
			return true;
	}
	//移动2G信号，联通2G，3G信号
	public int getGSM() {
		return GSM;
	}
	//电信3G信号
	public int getEVO() {
		return EVO;
	}
	//3g上传流量速度
	public long get3Gup() {
		return g3up;
	}
	//3g下载流量速度
	public long get3Gdown() {
		return g3down;
	}
	//gps开关状态
	public boolean getGpsEn() {
		return isGpsEnable;
	}
	//gps卫星数量
	public int getGpsNum() {
		return gpsNum;
	}
	
	public ThirdGeneration(Context context) {
		// setContentView(R.layout.activity_g3_example);

		// mLabel3G = (TextView) findViewById(R.id.Label_3GDetail);

		// 信号强度
		tel = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		tel.listen(new PhoneStateMonitor(),
				PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
						| PhoneStateListener.LISTEN_SERVICE_STATE);

		/**
		 * 卫星状态监听器
		 */

		locationManager = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);

		final GpsStatus.Listener statusListener = new GpsStatus.Listener() {
			public void onGpsStatusChanged(int event) { // GPS状态变化时的回调，如卫星数
				if (locationManager
						.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
					isGpsEnable = true;
				} else {
					isGpsEnable = false;
				}
				
				GpsStatus status = locationManager.getGpsStatus(null); // 取当前状态
				String satelliteInfo = updateGpsStatus(event, status);
				if ("".equals(satelliteInfo)) {
					gpsNum = 0;
				} else {
					gpsNum = Integer.parseInt(satelliteInfo);
				}
			}
		};

		locationManager.addGpsStatusListener(statusListener);

		//定义   
		
		LocationListener locationListener=new LocationListener(){  
			String a;
		    //位置信息变化时触发   
		    public void onLocationChanged(Location location) {  
//		        System.out.println("时间："+location.getTime());  
//		        System.out.println("经度："+location.getLongitude());  
//		        System.out.println("纬度："+location.getLatitude());  
//		        System.out.println("海拔："+location.getAltitude());  
		    }  
			
		    //gps禁用时触发   
		    public void onProviderDisabled(String provider) {  
		        isGpsEnable = false;
		    }  
		    //gps开启时触发   
		    public void onProviderEnabled(String provider) {  
		    	isGpsEnable = true;
		    }  
		    //gps状态变化时触发   
		    public void onStatusChanged(String provider, int status,Bundle extras) {  
//		        if(status==LocationProvider.AVAILABLE){  
//		            a = ("当前GPS状态：可见的\n");  
//		            Log.i("z",a);
//		        }else if(status==LocationProvider.OUT_OF_SERVICE){  
//		            a = ("当前GPS状态：服务区外\n");  
//		            Log.i("z",a);
//		        }else if(status==LocationProvider.TEMPORARILY_UNAVAILABLE){  
//		            a = ("当前GPS状态：暂停服务\n");  
//		            Log.i("z",a);
//		        }  
		    }  
		};  
		//绑定监听，有4个参数   
		//参数1，设备：有GPS_PROVIDER和NETWORK_PROVIDER两种，我们选用GPS，网络在此不做讨论   
		//参数2，位置信息更新周期：   
		//参数3，位置变化最小距离：当位置距离变化超过此值时，将更新位置信息   
		//参数4，监听   
		//备注：参数2和3，如果参数3不为0，则以参数3为准；参数3为0，则通过时间来定时更新；两者为0，则随时刷新   
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);   
		
		
		// 流量
		final Handler handler = new Handler() {
			//3g
			long gjtx = TrafficMonitoring.mReceive();
			long formergprs = TrafficMonitoring.mReceive();

			long gjtu = TrafficMonitoring.mSend();
			long formergprs2 = TrafficMonitoring.mSend();
			
			
			public void handleMessage(Message msg) {
				//3g
				formergprs = gjtx;
				gjtx = TrafficMonitoring.mReceive();
				g3up = gjtx - formergprs;
				//Log.i("up", g3up + "");

				formergprs2 = gjtu;
				gjtu = TrafficMonitoring.mSend();
				g3down = gjtu - formergprs2;
				//Log.i("down", g3down + "");
				
				
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

	private String updateGpsStatus(int event, GpsStatus status) {
		StringBuilder sb2 = new StringBuilder("");
		if (status == null) {
			sb2.append(0);
		} else if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS) {
			int maxSatellites = status.getMaxSatellites();
			Iterator<GpsSatellite> it = status.getSatellites().iterator();
			numSatelliteList.clear();
			int count = 0;
			while (it.hasNext() && count <= maxSatellites) {
				GpsSatellite s = it.next();
				numSatelliteList.add(s);
				count++;
			}
			sb2.append(numSatelliteList.size());
		}

		return sb2.toString();
	}

	public class PhoneStateMonitor extends PhoneStateListener {
		public void onSignalStrengthsChanged(SignalStrength signalStrength) {
			super.onSignalStrengthsChanged(signalStrength);

			GSM = signalStrength.getGsmSignalStrength() * 2 - 113;
			WCDMA = signalStrength.getCdmaDbm();
			EVO = signalStrength.getEvdoDbm();

			networktype = NetworkType(tel.getNetworkType());
//			Log.i("asd", GSM + "");
//			Log.i("WCDMA", WCDMA + "");
//			Log.i("EVO", EVO + "");
			/*
			 * signalStrength.isGsm() 是否GSM信号 2G or 3G
			 * signalStrength.getCdmaDbm(); 联通3G 信号强度
			 * signalStrength.getCdmaEcio(); 联通3G 载干比
			 * signalStrength.getEvdoDbm(); 电信3G 信号强度
			 * signalStrength.getEvdoEcio(); 电信3G 载干比
			 * signalStrength.getEvdoSnr(); 电信3G 信噪比
			 * signalStrength.getGsmSignalStrength(); 2G 信号强度
			 * signalStrength.getGsmBitErrorRate(); 2G 误码率
			 * 
			 * 载干比 ，它是指空中模拟电波中的信号与噪声的比值
			 */

			// // gsm
			// if (signalStrength.isGsm()) {
			// mLabel3G.setText("Gsm SignalStrength : "
			// + (signalStrength.getGsmSignalStrength()*2-113) + "Dbm");
			// }
			// // evo3G
			// else if (signalStrength.getEvdoDbm() != -1) {
			// mLabel3G.setText("Evdo Dbm : " + signalStrength.getEvdoDbm()
			// + "Dbm");
			// }
			// // wcdma
			// else {
			// mLabel3G.setText("WCDMA Dbm : " + signalStrength.getCdmaDbm()
			// + "Dbm");
			// }
		}

		public void onServiceStateChanged(ServiceState serviceState) {
			super.onServiceStateChanged(serviceState);

			/*
			 * ServiceState.STATE_EMERGENCY_ONLY 仅限紧急呼叫
			 * ServiceState.STATE_IN_SERVICE 信号正常
			 * ServiceState.STATE_OUT_OF_SERVICE 不在服务区
			 * ServiceState.STATE_POWER_OFF 断电
			 */
			switch (serviceState.getState()) {
			case ServiceState.STATE_EMERGENCY_ONLY:
				Log.d(TAG, "3G STATUS : STATE_EMERGENCY_ONLY");
				break;
			case ServiceState.STATE_IN_SERVICE:
				Log.d(TAG, "3G STATUS : STATE_IN_SERVICE");
				break;
			case ServiceState.STATE_OUT_OF_SERVICE:
				Log.d(TAG, "3G STATUS : STATE_OUT_OF_SERVICE");
				break;
			case ServiceState.STATE_POWER_OFF:
				Log.d(TAG, "3G STATUS : STATE_POWER_OFF");
				break;
			default:
				break;
			}
		}
	
		public int NetworkType(int a){
	        /**
	         * 获取网络类型
	         * 
	         * NETWORK_TYPE_CDMA 网络类型为CDMA
	         * NETWORK_TYPE_EDGE 网络类型为EDGE
	         * NETWORK_TYPE_EVDO_0 网络类型为EVDO0
	         * NETWORK_TYPE_EVDO_A 网络类型为EVDOA
	         * NETWORK_TYPE_GPRS 网络类型为GPRS
	         * NETWORK_TYPE_HSDPA 网络类型为HSDPA
	         * NETWORK_TYPE_HSPA 网络类型为HSPA
	         * NETWORK_TYPE_HSUPA 网络类型为HSUPA
	         * NETWORK_TYPE_UMTS 网络类型为UMTS
	         * 
	         * 在中国，联通的3G为UMTS或HSDPA，移动和联通的2G为GPRS或EGDE，电信的2G为CDMA，电信的3G为EVDO
	         */
			if(a == tel.NETWORK_TYPE_GPRS || a == tel.NETWORK_TYPE_EDGE)
				return 2;
			else
				return 3;
		}
	}

	public static boolean checkNet(Context context) {// 获取手机所有连接管理对象（包括对wi-fi,

		// net等连接的管理）

		try {

			ConnectivityManager connectivity = (ConnectivityManager) context

			.getSystemService(Context.CONNECTIVITY_SERVICE);

			if (connectivity != null) {

				// 获取网络连接管理的对象

				NetworkInfo info = connectivity.getActiveNetworkInfo();

				// System.out.println(">>>>>>>>>>>>Net:"+info);

				if (info == null || !info.isAvailable()) {

					return false;

				} else {

					return true;

				}

				// if (info != null && info.isConnected()) {

				// // 判断当前网络是否已经连接

				// if (info.getState() == NetworkInfo.State.CONNECTED) {

				// return true;

				// }

				// }

			}

		} catch (Exception e) {

			e.printStackTrace();

		}

		return false;

	}
}