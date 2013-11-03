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
	// �ź�ǿ��
	public int GSM;
	public int WCDMA;
	public int EVO;
	//�ź�����
	public int networktype;
	// 3g�ϴ������ٶ�
	public long g3up;
	// 3g���������ٶ�
	public long g3down;
	
	// gps״̬
	public boolean isGpsEnable;
	public int gpsNum;
	TelephonyManager tel;
	List<GpsSatellite> numSatelliteList = new ArrayList<GpsSatellite>(); // �����ź�
	LocationManager locationManager;

	

	//��ͨ2G����3G
	public boolean is3G(){
		if(networktype == 2)
			return false;
		else
			return true;
	}
	//�ƶ�2G�źţ���ͨ2G��3G�ź�
	public int getGSM() {
		return GSM;
	}
	//����3G�ź�
	public int getEVO() {
		return EVO;
	}
	//3g�ϴ������ٶ�
	public long get3Gup() {
		return g3up;
	}
	//3g���������ٶ�
	public long get3Gdown() {
		return g3down;
	}
	//gps����״̬
	public boolean getGpsEn() {
		return isGpsEnable;
	}
	//gps��������
	public int getGpsNum() {
		return gpsNum;
	}
	
	public ThirdGeneration(Context context) {
		// setContentView(R.layout.activity_g3_example);

		// mLabel3G = (TextView) findViewById(R.id.Label_3GDetail);

		// �ź�ǿ��
		tel = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		tel.listen(new PhoneStateMonitor(),
				PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
						| PhoneStateListener.LISTEN_SERVICE_STATE);

		/**
		 * ����״̬������
		 */

		locationManager = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);

		final GpsStatus.Listener statusListener = new GpsStatus.Listener() {
			public void onGpsStatusChanged(int event) { // GPS״̬�仯ʱ�Ļص�����������
				if (locationManager
						.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
					isGpsEnable = true;
				} else {
					isGpsEnable = false;
				}
				
				GpsStatus status = locationManager.getGpsStatus(null); // ȡ��ǰ״̬
				String satelliteInfo = updateGpsStatus(event, status);
				if ("".equals(satelliteInfo)) {
					gpsNum = 0;
				} else {
					gpsNum = Integer.parseInt(satelliteInfo);
				}
			}
		};

		locationManager.addGpsStatusListener(statusListener);

		//����   
		
		LocationListener locationListener=new LocationListener(){  
			String a;
		    //λ����Ϣ�仯ʱ����   
		    public void onLocationChanged(Location location) {  
//		        System.out.println("ʱ�䣺"+location.getTime());  
//		        System.out.println("���ȣ�"+location.getLongitude());  
//		        System.out.println("γ�ȣ�"+location.getLatitude());  
//		        System.out.println("���Σ�"+location.getAltitude());  
		    }  
			
		    //gps����ʱ����   
		    public void onProviderDisabled(String provider) {  
		        isGpsEnable = false;
		    }  
		    //gps����ʱ����   
		    public void onProviderEnabled(String provider) {  
		    	isGpsEnable = true;
		    }  
		    //gps״̬�仯ʱ����   
		    public void onStatusChanged(String provider, int status,Bundle extras) {  
//		        if(status==LocationProvider.AVAILABLE){  
//		            a = ("��ǰGPS״̬���ɼ���\n");  
//		            Log.i("z",a);
//		        }else if(status==LocationProvider.OUT_OF_SERVICE){  
//		            a = ("��ǰGPS״̬����������\n");  
//		            Log.i("z",a);
//		        }else if(status==LocationProvider.TEMPORARILY_UNAVAILABLE){  
//		            a = ("��ǰGPS״̬����ͣ����\n");  
//		            Log.i("z",a);
//		        }  
		    }  
		};  
		//�󶨼�������4������   
		//����1���豸����GPS_PROVIDER��NETWORK_PROVIDER���֣�����ѡ��GPS�������ڴ˲�������   
		//����2��λ����Ϣ�������ڣ�   
		//����3��λ�ñ仯��С���룺��λ�þ���仯������ֵʱ��������λ����Ϣ   
		//����4������   
		//��ע������2��3���������3��Ϊ0�����Բ���3Ϊ׼������3Ϊ0����ͨ��ʱ������ʱ���£�����Ϊ0������ʱˢ��   
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);   
		
		
		// ����
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
			 * signalStrength.isGsm() �Ƿ�GSM�ź� 2G or 3G
			 * signalStrength.getCdmaDbm(); ��ͨ3G �ź�ǿ��
			 * signalStrength.getCdmaEcio(); ��ͨ3G �ظɱ�
			 * signalStrength.getEvdoDbm(); ����3G �ź�ǿ��
			 * signalStrength.getEvdoEcio(); ����3G �ظɱ�
			 * signalStrength.getEvdoSnr(); ����3G �����
			 * signalStrength.getGsmSignalStrength(); 2G �ź�ǿ��
			 * signalStrength.getGsmBitErrorRate(); 2G ������
			 * 
			 * �ظɱ� ������ָ����ģ��粨�е��ź��������ı�ֵ
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
			 * ServiceState.STATE_EMERGENCY_ONLY ���޽�������
			 * ServiceState.STATE_IN_SERVICE �ź�����
			 * ServiceState.STATE_OUT_OF_SERVICE ���ڷ�����
			 * ServiceState.STATE_POWER_OFF �ϵ�
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
	         * ��ȡ��������
	         * 
	         * NETWORK_TYPE_CDMA ��������ΪCDMA
	         * NETWORK_TYPE_EDGE ��������ΪEDGE
	         * NETWORK_TYPE_EVDO_0 ��������ΪEVDO0
	         * NETWORK_TYPE_EVDO_A ��������ΪEVDOA
	         * NETWORK_TYPE_GPRS ��������ΪGPRS
	         * NETWORK_TYPE_HSDPA ��������ΪHSDPA
	         * NETWORK_TYPE_HSPA ��������ΪHSPA
	         * NETWORK_TYPE_HSUPA ��������ΪHSUPA
	         * NETWORK_TYPE_UMTS ��������ΪUMTS
	         * 
	         * ���й�����ͨ��3GΪUMTS��HSDPA���ƶ�����ͨ��2GΪGPRS��EGDE�����ŵ�2GΪCDMA�����ŵ�3GΪEVDO
	         */
			if(a == tel.NETWORK_TYPE_GPRS || a == tel.NETWORK_TYPE_EDGE)
				return 2;
			else
				return 3;
		}
	}

	public static boolean checkNet(Context context) {// ��ȡ�ֻ��������ӹ�����󣨰�����wi-fi,

		// net�����ӵĹ���

		try {

			ConnectivityManager connectivity = (ConnectivityManager) context

			.getSystemService(Context.CONNECTIVITY_SERVICE);

			if (connectivity != null) {

				// ��ȡ�������ӹ���Ķ���

				NetworkInfo info = connectivity.getActiveNetworkInfo();

				// System.out.println(">>>>>>>>>>>>Net:"+info);

				if (info == null || !info.isAvailable()) {

					return false;

				} else {

					return true;

				}

				// if (info != null && info.isConnected()) {

				// // �жϵ�ǰ�����Ƿ��Ѿ�����

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