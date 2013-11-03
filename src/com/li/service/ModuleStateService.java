package com.li.service;


import android.app.ActivityManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.TrafficStats;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryStats;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.os.BatteryStatsImpl;
import com.li.helpclass.CpuInfo;
import com.li.helpclass.ExternalStorage;
import com.li.helpclass.MemInfo;
import com.li.helpclass.ReadSystemMemory;
import com.li.helpclass.ReadWriteFile;
import com.li.helpclass.ThirdGeneration;
import com.li.helpclass.WifiFlow;
import com.li.power.PowerUsage;

public class ModuleStateService extends Service{
	
	private String cpuInfo=null;
	private String memInfo=null;
	private String phoneInfo=null;
	private String audioInfo=null;
	private String screenInfo=null;
	private String wifiInfo=null;
	private String bluetoothInfo=null;
	private String GPSInfo=null;
	private String _3GInfo=null;
	private String information=null;	
	private String currentTime=null;
	private String storageInfo=null;
	private int SERVER_PORT = 8100;//端口
	private final String TAG="ModuleState";
	private Thread td=null;
	private Thread tdBattery=null;
	private final String MODULEPATH = "/mnt/sdcard/moduleState.txt";
	private String moduleStateInfo=null;
	public static boolean condition=true;//用于控制进程的循环
	//private BatteryReceiver receiver=new BatteryReceiver();
    BatteryStatsImpl mStats;
    BatteryStats mStatss;

	ThirdGeneration thirdGeneration=null;
	WifiFlow wififlow;
	TelephonyManager tem=null;//获取手机通话状态
	AudioManager aum=null;
	private WifiManager wim=null;//WIFI状态
	private ConnectivityManager com=null;//连接状态	
	private AudioManager am=null;//音频管理器
	private BluetoothAdapter bla=null;//蓝牙适配器
	LocationManager lom=null;//GPS位置管理
	ContentResolver resolver=null;//获取屏幕亮度
	PowerManager pom =null;//判断是否亮屏
	PowerManager.WakeLock wl=null;//防止手机休眠 ;
	int gsignal=0;//signalStrength.getCdmaDbm();
	int gsmsingal=0;//signalStrength.getGsmSignalStrength();//信号强度
	long wold=-1,gold=-1;
	String BatteryInfo=null;//用于获取电池状态信息
	PowerUsage powerUsage;
	
	/*自己写的辅助类*/
	CpuInfo cpuString= new CpuInfo();
	ReadWriteFile readWriteFile=new ReadWriteFile();
	ExternalStorage externalStorage=new ExternalStorage();
	
	MyPhoneStateListener   MyListener   = new MyPhoneStateListener();
	TelephonyManager tm;
    ActivityManager AM;
    ReadSystemMemory mem;
	class MyPhoneStateListener extends PhoneStateListener
    {
      @Override
      public void onSignalStrengthsChanged(SignalStrength signalStrength)
      {
         super.onSignalStrengthsChanged(signalStrength);
         gsignal=signalStrength.getCdmaDbm();
         gsignal= signalStrength.getCdmaLevel();
         gsignal=signalStrength.getCdmaAsuLevel();
       
         gsmsingal=signalStrength.getGsmSignalStrength();
      }
    };
    
//    private class BatteryReceiver extends BroadcastReceiver{
//		@Override
//		public void onReceive(Context context, Intent intent) {
//			// TODO Auto-generated method stub
//			int current=intent.getExtras().getInt("level");//get the current power
//            int total=intent.getExtras().getInt("scale");//get the total power
//            float percent=current*100/total;
//            int voltage = intent.getIntExtra("voltage", 0);
//            int temperature = intent.getIntExtra("temperature", 0);
//            int plugged = intent.getIntExtra("plugged", 0);
//            String PowerSupply=null;
//            if(plugged==1){
//            	PowerSupply="AC供电";
//            }else{
//            	PowerSupply="USB供电";
//            }
//            String temp = ","+current +","+temperature+","+PowerSupply;  
//            BatteryInfo=temp;
//		}
//    }
       	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void onCreate(){
		super.onCreate();
		tem = (TelephonyManager)getSystemService(Service.TELEPHONY_SERVICE);
		aum= (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		wim=(WifiManager)getSystemService(Context.WIFI_SERVICE);		
		com = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);	
		bla=BluetoothAdapter.getDefaultAdapter();
		lom=(LocationManager)getSystemService(Context.LOCATION_SERVICE); 
		
		resolver = getContentResolver();//获取屏幕的亮度值
		
		pom = (PowerManager)getSystemService(Context.POWER_SERVICE);
		wl = pom.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Partial wakelock");//初始化一把partialwakelock
	
		AM=(ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);//用来算取内存利用率的
		mem= new ReadSystemMemory(AM,this);
		
	    powerUsage=new PowerUsage(ModuleStateService.this);//用于统计手机的各部分功耗
	    thirdGeneration=new ThirdGeneration(ModuleStateService.this);//用于判
	    wififlow = new WifiFlow(ModuleStateService.this);
		System.out.println("Service is Created");
		
	}

    
	public int onStartCommand(Intent intent, int flags ,int startId){
	
		wl.acquire();
		
//		IntentFilter filter=new IntentFilter(Intent.ACTION_BATTERY_CHANGED);//监听电池的状态信息
//     registerReceiver(receiver, filter);  
        
		tem.listen(MyListener ,PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);//监听手机的信号
		getInformation();//开始抓取信息		
		
		return START_STICKY;
		
	}
	public void onDestroy(){
		super.onDestroy();
		if (condition == false) { // 在循环结束的时候释放锁
		if (wl != null) {
			wl.release();
			wl = null;
			Log.i(TAG, "onDestroy()中锁已释放");
		}
}
//		unregisterReceiver(receiver); 
		System.out.println("Service is Destroyed");
	}
	
   

	public int getCallState() //idle,ring,offhook.
	{
		return tem.getCallState();
	}
	public int getPhoneType()   //gsm,cdma
	{
		return tem.getPhoneType();
	}
    public int getGsignal(){//3G信号强度
    	Log.i(TAG,String.valueOf(gsignal));
    	return gsignal;
    }
    public int getGsmsignal(){//2G信号强度
    	return gsmsingal;
    }
    
	public int WifiRssi()//wifi信号强度
	{
		WifiInfo info=wim.getConnectionInfo();
		return info.getRssi();
	}
	public String WifiLinkSpeed()//wifi连接速率
	{
		WifiInfo info=wim.getConnectionInfo();
		return info.getLinkSpeed()+"Mbps";
	}
	public int wifiIsConnected(){//判断wifi模块是否连接到网络
		ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (mWifi.isConnected()) {
		    return 5;
		}else{
			return 6;
		}
	}
	public int getWifiState()//wifi状态
	{
		return wim.getWifiState();
	}
	public double FlowOfWifi()//wifi数据流
	{
		State wifi = com.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState();
		if(wifi==State.CONNECTED||wifi==State.CONNECTING)		
		{
			long to=RecvFlowWifi()+SendFlowWifi();
			if(wold==-1)
			{
				wold=to;
				return 0;
			}
			long outto=to-wold;
			wold=to;
			if(outto<0)
				return 0;
			return outto;
		}
		else
		{
			return 0;
		}		
	}
	public long FlowOf3G()//3G数据流
	{
		State mobile = com.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState();
		if(mobile == State.CONNECTED||mobile==State.CONNECTING)
		{
			long to=RecvFlow3G()+SendFlow3G();
			if(gold==-1)
			{
				gold=to;
				return 0;
			}
			long outto=to-gold;
			gold=to;
			if(outto<=0)
				return 0;
			return outto;
		}
		else
		{
			return 0;
		}		
	}
	private long RecvFlow3G()
	{		
		return TrafficStats.getMobileRxBytes()/1024;			
	}
	private long RecvFlowWifi()
	{
		return (TrafficStats.getTotalRxBytes()-TrafficStats.getMobileRxBytes())/1024;
	}
	private long SendFlow3G()
	{
		return TrafficStats.getMobileTxBytes()/1024;
	}
	private long SendFlowWifi()   //kB
	{
		return (TrafficStats.getTotalTxBytes()-TrafficStats.getMobileTxBytes())/1024;
	}

	public boolean isBluetoothEnabled()//蓝牙
	{
		return bla.isEnabled();
	}
	public int getBluetoothState()//获取蓝牙状态
	{
		return bla.getState();
	}
	public String isGpsEnabled()//GPS是否可用
	{
		 if(lom.isProviderEnabled(LocationManager.GPS_PROVIDER)){
			 return "1";
		 }else{
			 return "0";
		 }
	}
	public int getSbValue()//用来获取屏幕的亮度值
	{
		int nowBrightnessValue=0;
		try {
			nowBrightnessValue = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS);
		} catch (Exception e) {
		}
		if(pom.isScreenOn())
		    return nowBrightnessValue;
		else
			return 0;
	}
	public int IsScreenOn(){//判断屏幕是否开启
		if(pom.isScreenOn())
		    return 1;
		else
			return 0;
	}
	public int ishead()
	{
		if( aum.isWiredHeadsetOn()){
			return 1;
		}else{
			return 0;
		}
		
	}
	public int isMusic()
	{
		 if(aum.isMusicActive()){
			 return 1;
		 }else{
			 return 0;
		 }
	}
	public int getMusicVolume()
	{
		if(isMusic()==1){
		return aum.getStreamVolume(AudioManager.STREAM_MUSIC);
		}else{
			return 0;
		}
	}
	public int getVoiceCallVolume()
	{
		return aum.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
	}
	public void getDevicePower(){
		tdBattery = new Thread(new Runnable(){
		       	public void run(){
		       		powerUsage.getBluetoothUsage(powerUsage.getSectime());
		       		powerUsage.getCPUIdleUsage(powerUsage.getSectime());
		       		powerUsage.getPhoneUsage(powerUsage.getSectime());
		       		powerUsage.getScreenUsage(powerUsage.getSectime());
		       		powerUsage.getWiFiUsage(powerUsage.getSectime());
		       		powerUsage.getRadioUsage(powerUsage.getSectime());	       		
		       		}		       				       	
		       });
		tdBattery.start();
	}
	public void getInformation(){
		 new Thread(new Runnable(){
       	public void run(){
       		int i=1;//防止首次写入null
       		while(condition){
     			if(i%6 == 0){
     				
     				moduleStateInfo =moduleStateInfo+ gatherInformation()+"\r"+"\n";
     				readWriteFile.writeFile(moduleStateInfo, MODULEPATH);
     				Log.i(TAG,gatherInformation());	
     				moduleStateInfo = "";
     				
     			}else{ 				
     				moduleStateInfo =moduleStateInfo+ gatherInformation()+"\r"+"\n";
     				Log.i(TAG,gatherInformation());				
     			} 			
           		try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
           		i++;
       		}
       	}
       }).start();
	}
	public String gatherInformation(){		
		
//		long a = memInfo.ge
		currentTime=readWriteFile.getNowTime();
		
		cpuInfo=","+cpuString.getCurCpuFreq()+","+((int)(cpuString.getCpuUsage()*100))+","+cpuString.getCurCpuCoreNum(); //new java.text.DecimalFormat("#.000").format(cpuString.getCpuUsage());
		
		memInfo=","+ MemInfo.getmem_UNUSED(this)+","+ MemInfo.getmem_TOLAL()+","+MemInfo.usageMem(this);
				
		storageInfo=","+externalStorage.getTotalBlock()+","+externalStorage.getAvailableBlock()+","+externalStorage.getBlockUsage();
		
		screenInfo=","+IsScreenOn()+","+getSbValue();
		
		phoneInfo=","+thirdGeneration.is3G()+","+tem.getCallState()+","+thirdGeneration.getGSM()+","+thirdGeneration.get3Gup()+","+thirdGeneration.get3Gdown();
		
		wifiInfo=","+getWifiState()+","+WifiRssi()+","+WifiLinkSpeed()+","+wififlow.getwifiup()+","+wififlow.getwifidown()+","+thirdGeneration.checkNet(this);
		
		audioInfo=","+ishead()+","+getMusicVolume()+","+getVoiceCallVolume();
				
		GPSInfo=","+isGpsEnabled()+","+thirdGeneration.getGpsNum();
		
		bluetoothInfo=","+getBluetoothState();
		
		information=currentTime+cpuInfo+memInfo+screenInfo+phoneInfo+wifiInfo+storageInfo+audioInfo+bluetoothInfo+GPSInfo;//+BatteryInfo;
		
		Log.i(TAG,memInfo);
		Log.i("nice", cpuInfo);
//		Log.i(TAG,"CPU当前频率："+cpuString.getCurCpuFreq()+"  "+"CPU当前利用率:"+((int)(cpuString.getCpuUsage()*100)));
//		Log.i(TAG,"系统可用内存："+mem.getAvailMemory()+"  "+"系统全部内存："+mem.getTotalMemory()+"内存利用率："+mem.getMemoryUsage());	
//		Log.i(TAG,"屏幕是否打开："+IsScreenOn()+","+"屏幕亮度："+getSbValue());
//		Log.i(TAG,"3G模块是否打开："+thirdGeneration.is3G()+"  "+"系统通话状态："+tem.getCallState()+"手机信号强度："+thirdGeneration.getGSM());
//		Log.i(TAG,"3G上传流量："+thirdGeneration.get3Gup()+"  "+"3G下载流量："+thirdGeneration.get3Gdown());
//		Log.i(TAG,"WIFI状态："+getWifiState()+"  "+"WIFI信号强度："+WifiRssi()+" "+"WIFI连接速率:"+WifiLinkSpeed());
//		Log.i(TAG,"WIFI上传流量："+SendFlowWifi()+"  "+"WIFI下载流量："+RecvFlowWifi());
//		Log.i(TAG,"是否接入耳机："+ishead()+"  "+"多媒体音量大小："+getMusicVolume()+" "+"语音通话音量大小:"+getVoiceCallVolume());
//		Log.i(TAG, "总体Block:"+externalStorage.getTotalBlock()+" 可用BLock："+externalStorage.getAvailableBlock()+"Block使用率："+externalStorage.getBlockUsage());
//		Log.i(TAG,"GPS状态："+isGpsEnabled()+" "+"可搜到的GPS卫星数："+thirdGeneration.getGpsNum());
//		Log.i(TAG,"蓝牙状态："+getBluetoothState());
		
		
		
		return information;
	}
}
