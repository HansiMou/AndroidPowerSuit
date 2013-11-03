package com.li.power;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.hardware.SensorManager;
import android.os.BatteryStats;
import android.os.BatteryStats.Uid;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;

import com.android.internal.app.IBatteryStats;
import com.android.internal.os.BatteryStatsImpl;
import com.android.internal.os.PowerProfile;

public class PowerUsage {
	private static final boolean DEBUG = false;
	private static final String TAG = "PowerSummary";
	IBatteryStats mBatteryInfo;
	BatteryStatsImpl mStats;

	private int mStatsType = BatteryStats.STATS_SINCE_CHARGED;

	private static final int MIN_POWER_THRESHOLD = 5;
	private static final int MAX_ITEMS_TO_LIST = 10;

	private long mStatsPeriod = 0;
	private double mMaxPower = 1;
	private double mTotalPower;
	private double mWifiPower;
	private double mBluetoothPower;
	private PowerProfile mPowerProfile;

	// How much the apps together have left WIFI running.
	private long mAppWifiRunning;

	/** Queue for fetching name and icon for an application */

	private Thread mRequestThread;
	private boolean mAbort;

	public HashMap processAppUsage(Context context) {//MAP中包含应用名称和对应的功耗,功耗单位是毫安时
		SensorManager sensorManager = (SensorManager) context
				.getSystemService(Context.SENSOR_SERVICE);
		PackageManager pm = context.getPackageManager();// 实现包名和应用名称的转换
		ApplicationInfo ai;
		HashMap hm = new HashMap();
		String AppUsage = null;
		final int which = mStatsType;
		final int speedSteps = mPowerProfile.getNumSpeedSteps();
		final double[] powerCpuNormal = new double[speedSteps];
		final long[] cpuSpeedStepTimes = new long[speedSteps];
		for (int p = 0; p < speedSteps; p++) {
			powerCpuNormal[p] = mPowerProfile.getAveragePower(
					PowerProfile.POWER_CPU_ACTIVE, p);
		}
		final double averageCostPerByte = getAverageDataCost();
		long uSecTime = mStats.computeBatteryRealtime(
				SystemClock.elapsedRealtime() * 1000, which);
		long appWakelockTime = 0;
		mStatsPeriod = uSecTime;// ? extends Uid java泛型语法
		SparseArray<? extends Uid> uidStats = mStats.getUidStats();// 以活动的应用程序为单位进行统计
		final int NU = uidStats.size();
		for (int iu = 0; iu < NU; iu++) {
			String packageName = null;// 用于记录包名
			Uid u = uidStats.valueAt(iu);
			double power = 0;
			double highestDrain = 0;
			String packageWithHighestDrain = null;
			// mUsageList.add(new AppUsage(u.getUid(), new double[] {power}));
			Map<String, ? extends BatteryStats.Uid.Proc> processStats = u
					.getProcessStats();
			long cpuTime = 0;
			long cpuFgTime = 0;
			long wakelockTime = 0;
			long gpsTime = 0;
			if (processStats.size() > 0) {
				// Process CPU time
				for (Map.Entry<String, ? extends BatteryStats.Uid.Proc> ent : processStats
						.entrySet()) {
					if (DEBUG)
						Log.i(TAG, "Process name = " + ent.getKey());// 已包名为键吗？
					packageName = ent.getKey();
					Uid.Proc ps = ent.getValue();
					final long userTime = ps.getUserTime(which);
					final long systemTime = ps.getSystemTime(which);
					final long foregroundTime = ps.getForegroundTime(which);
					cpuFgTime += foregroundTime * 10; // convert to millis
					final long tmpCpuTime = (userTime + systemTime) * 10; // convert
																			// to
																			// millis
					int totalTimeAtSpeeds = 0;
					// Get the total first
					for (int step = 0; step < speedSteps; step++) {
						cpuSpeedStepTimes[step] = ps.getTimeAtCpuSpeedStep(
								step, which);
						totalTimeAtSpeeds += cpuSpeedStepTimes[step];
					}
					if (totalTimeAtSpeeds == 0)
						totalTimeAtSpeeds = 1;
					// Then compute the ratio of time spent at each speed
					double processPower = 0;
					for (int step = 0; step < speedSteps; step++) {
						double ratio = (double) cpuSpeedStepTimes[step]
								/ totalTimeAtSpeeds;
						processPower += ratio * tmpCpuTime
								* powerCpuNormal[step];
					}
					cpuTime += tmpCpuTime;
					power += processPower;
					if (packageWithHighestDrain == null
							|| packageWithHighestDrain.startsWith("*")) {
						highestDrain = processPower;
						packageWithHighestDrain = ent.getKey();
					} else if (highestDrain < processPower
							&& !ent.getKey().startsWith("*")) {
						highestDrain = processPower;
						packageWithHighestDrain = ent.getKey();
					}
				}
				if (DEBUG)
					Log.i(TAG, "Max drain of " + highestDrain + " by "
							+ packageWithHighestDrain);
			}
			if (cpuFgTime > cpuTime) {
				if (DEBUG && cpuFgTime > cpuTime + 10000) {
					Log.i(TAG,
							"WARNING! Cputime is more than 10 seconds behind Foreground time");
				}
				cpuTime = cpuFgTime; // Statistics may not have been gathered
										// yet.
			}
			power /= 1000;

			// Process wake lock usage
			Map<String, ? extends BatteryStats.Uid.Wakelock> wakelockStats = u
					.getWakelockStats();
			for (Map.Entry<String, ? extends BatteryStats.Uid.Wakelock> wakelockEntry : wakelockStats
					.entrySet()) {
				Uid.Wakelock wakelock = wakelockEntry.getValue();
				// Only care about partial wake locks since full wake locks
				// are canceled when the user turns the screen off.
				BatteryStats.Timer timer = wakelock
						.getWakeTime(BatteryStats.WAKE_TYPE_PARTIAL);
				if (timer != null) {
					wakelockTime += timer.getTotalTimeLocked(uSecTime, which);
				}
			}
			wakelockTime /= 1000; // convert to millis
			appWakelockTime += wakelockTime;

			// Add cost of holding a wake lock
			power += (wakelockTime * mPowerProfile
					.getAveragePower(PowerProfile.POWER_CPU_AWAKE)) / 1000;

			// Add cost of data traffic
			long tcpBytesReceived = u.getTcpBytesReceived(mStatsType);
			long tcpBytesSent = u.getTcpBytesSent(mStatsType);
			power += (tcpBytesReceived + tcpBytesSent) * averageCostPerByte;

			// Add cost of keeping WIFI running.
			long wifiRunningTimeMs = u.getWifiRunningTime(uSecTime, which) / 1000;
			mAppWifiRunning += wifiRunningTimeMs;
			power += (wifiRunningTimeMs * mPowerProfile
					.getAveragePower(PowerProfile.POWER_WIFI_ON)) / 1000;

			// Process Sensor usage
			Map<Integer, ? extends BatteryStats.Uid.Sensor> sensorStats = u
					.getSensorStats();
			for (Map.Entry<Integer, ? extends BatteryStats.Uid.Sensor> sensorEntry : sensorStats
					.entrySet()) {
				Uid.Sensor sensor = sensorEntry.getValue();
				int sensorType = sensor.getHandle();
				BatteryStats.Timer timer = sensor.getSensorTime();
				long sensorTime = timer.getTotalTimeLocked(uSecTime, which) / 1000;
				double multiplier = 0;
				switch (sensorType) {
				case Uid.Sensor.GPS:
					multiplier = mPowerProfile
							.getAveragePower(PowerProfile.POWER_GPS_ON);
					gpsTime = sensorTime;
					break;
				default:
					android.hardware.Sensor sensorData = sensorManager
							.getDefaultSensor(sensorType);
					if (sensorData != null) {
						multiplier = sensorData.getPower();
						if (DEBUG) {
							Log.i(TAG, "Got sensor " + sensorData.getName()
									+ " with power = " + multiplier);
						}
					}
				}
				power += (multiplier * sensorTime) / 1000;
			}

			if (DEBUG)
				Log.i(TAG, "UID " + u.getUid() + ": power=" + power);
			/* 包名称和应用名称之间的转换 */
			if (power >= 1) {
				try {
					ai = pm.getApplicationInfo(packageName, 0);
				} catch (final NameNotFoundException e) {
					ai = null;
				}
				String applicationName = (String) (ai != null ? pm
						.getApplicationLabel(ai) : "(unknown)");
				Log.i(TAG, "UID " + u.getUid() + ":" + packageName + ","
						+ applicationName + ": power=" + power);
				/* 在此处可将个Process的耗电量加入MAP中 */
				if(!packageName.contains("unknown"))
				hm.put(applicationName, (int) (power * 10) / 10);
			}
		}
		return hm;
	}

	public double GetGPSUsage(Context context) {// 获取GPS耗电量
		SensorManager sensorManager = (SensorManager) context
				.getSystemService(Context.SENSOR_SERVICE);

		final int which = mStatsType;
		final int speedSteps = mPowerProfile.getNumSpeedSteps();
		final double[] powerCpuNormal = new double[speedSteps];
		final long[] cpuSpeedStepTimes = new long[speedSteps];
		for (int p = 0; p < speedSteps; p++) {
			powerCpuNormal[p] = mPowerProfile.getAveragePower(
					PowerProfile.POWER_CPU_ACTIVE, p);
		}

		long uSecTime = mStats.computeBatteryRealtime(
				SystemClock.elapsedRealtime() * 1000, which);
		double power = 0;
		mStatsPeriod = uSecTime;// ? extends Uid java泛型语法
		SparseArray<? extends Uid> uidStats = mStats.getUidStats();// 以应用程序为单位进行统计
		final int NU = uidStats.size();
		for (int iu = 0; iu < NU; iu++) {
			Uid u = uidStats.valueAt(iu);

			Map<String, ? extends BatteryStats.Uid.Proc> processStats = u
					.getProcessStats();
			long gpsTime = 0;
			if (processStats.size() > 0) {
				// Process Sensor usage
				Map<Integer, ? extends BatteryStats.Uid.Sensor> sensorStats = u
						.getSensorStats();
				for (Map.Entry<Integer, ? extends BatteryStats.Uid.Sensor> sensorEntry : sensorStats
						.entrySet()) {
					Uid.Sensor sensor = sensorEntry.getValue();
					int sensorType = sensor.getHandle();
					BatteryStats.Timer timer = sensor.getSensorTime();
					long sensorTime = timer.getTotalTimeLocked(uSecTime, which) / 1000;
					double multiplier = 0;
					if (Uid.Sensor.GPS == sensorType) {
						multiplier = mPowerProfile
								.getAveragePower(PowerProfile.POWER_GPS_ON);
						gpsTime = sensorTime;
					}
					;
					power += (multiplier * gpsTime) / 1000;
				}

				if (DEBUG)
					Log.i(TAG, "UID " + u.getUid() + ": power=" + power);
			}
		}
		return power;
	}

	public double getCPUUsage(Context context) {
		final int which = mStatsType;
		final int speedSteps = mPowerProfile.getNumSpeedSteps();
		final double[] powerCpuNormal = new double[speedSteps];
		final long[] cpuSpeedStepTimes = new long[speedSteps];
		for (int p = 0; p < speedSteps; p++) {
			powerCpuNormal[p] = mPowerProfile.getAveragePower(
					PowerProfile.POWER_CPU_ACTIVE, p);
		}
		final double averageCostPerByte = getAverageDataCost();
		long uSecTime = mStats.computeBatteryRealtime(
				SystemClock.elapsedRealtime() * 1000, which);
		long appWakelockTime = 0;
		double power = 0;// 通过循环，计算CPU的功耗
		mStatsPeriod = uSecTime;// ? extends Uid java泛型语法
		SparseArray<? extends Uid> uidStats = mStats.getUidStats();// 以活动的应用程序为单位进行统计
		final int NU = uidStats.size();
		for (int iu = 0; iu < NU; iu++) {
			Uid u = uidStats.valueAt(iu);
			// double power = 0;
			double highestDrain = 0;
			String packageWithHighestDrain = null;
			// mUsageList.add(new AppUsage(u.getUid(), new double[] {power}));
			Map<String, ? extends BatteryStats.Uid.Proc> processStats = u
					.getProcessStats();
			long cpuTime = 0;
			long cpuFgTime = 0;
			long wakelockTime = 0;
			long gpsTime = 0;
			if (processStats.size() > 0) {
				// Process CPU time
				for (Map.Entry<String, ? extends BatteryStats.Uid.Proc> ent : processStats
						.entrySet()) {
					if (DEBUG)
						Log.i(TAG, "Process name = " + ent.getKey());// 已包名为键吗？
					Uid.Proc ps = ent.getValue();
					final long userTime = ps.getUserTime(which);
					final long systemTime = ps.getSystemTime(which);
					final long foregroundTime = ps.getForegroundTime(which);
					cpuFgTime += foregroundTime * 10; // convert to millis
					final long tmpCpuTime = (userTime + systemTime) * 10; // convert
																			// to
																			// millis
					int totalTimeAtSpeeds = 0;
					// Get the total first
					for (int step = 0; step < speedSteps; step++) {
						cpuSpeedStepTimes[step] = ps.getTimeAtCpuSpeedStep(
								step, which);
						totalTimeAtSpeeds += cpuSpeedStepTimes[step];
					}
					if (totalTimeAtSpeeds == 0)
						totalTimeAtSpeeds = 1;
					// Then compute the ratio of time spent at each speed
					double processPower = 0;
					for (int step = 0; step < speedSteps; step++) {
						double ratio = (double) cpuSpeedStepTimes[step]
								/ totalTimeAtSpeeds;
						processPower += ratio * tmpCpuTime
								* powerCpuNormal[step];
					}
					cpuTime += tmpCpuTime;
					power += processPower;
					if (packageWithHighestDrain == null
							|| packageWithHighestDrain.startsWith("*")) {
						highestDrain = processPower;
						packageWithHighestDrain = ent.getKey();
					} else if (highestDrain < processPower
							&& !ent.getKey().startsWith("*")) {
						highestDrain = processPower;
						packageWithHighestDrain = ent.getKey();
					}
				}
				if (DEBUG)
					Log.i(TAG, "Max drain of " + highestDrain + " by "
							+ packageWithHighestDrain);
			}
			if (cpuFgTime > cpuTime) {
				if (DEBUG && cpuFgTime > cpuTime + 10000) {
					Log.i(TAG,
							"WARNING! Cputime is more than 10 seconds behind Foreground time");
				}
				cpuTime = cpuFgTime; // Statistics may not have been gathered
										// yet.
			}

			if (DEBUG)
				Log.i(TAG, "UID " + u.getUid() + ": power=" + power);
		}
		if (DEBUG)
			Log.i(TAG, "CPU  power= " + power);
		return (power / 1000);
	}

	public double getPhoneUsage(long uSecNow) {// 手机的通话功耗
		long phoneOnTimeMs = mStats.getPhoneOnTime(uSecNow, mStatsType) / 1000;
		double phoneOnPower = mPowerProfile
				.getAveragePower(PowerProfile.POWER_RADIO_ACTIVE)
				* phoneOnTimeMs / 1000;
		if (DEBUG)
			Log.i(TAG, "phoneOn  power is " + phoneOnPower);
		return phoneOnPower;
	}

	public double getScreenUsage(long uSecNow) {// 获取屏幕的功耗值
		double power = 0;
		long screenOnTimeMs = mStats.getScreenOnTime(uSecNow, mStatsType) / 1000;
		power += screenOnTimeMs
				* mPowerProfile.getAveragePower(PowerProfile.POWER_SCREEN_ON);
		final double screenFullPower = mPowerProfile
				.getAveragePower(PowerProfile.POWER_SCREEN_FULL);
		for (int i = 0; i < BatteryStats.NUM_SCREEN_BRIGHTNESS_BINS; i++) {
			double screenBinPower = screenFullPower * (i + 0.5f)
					/ BatteryStats.NUM_SCREEN_BRIGHTNESS_BINS;
			long brightnessTime = mStats.getScreenBrightnessTime(i, uSecNow,
					mStatsType) / 1000;
			power += screenBinPower * brightnessTime;
			if (DEBUG) {
				Log.i(TAG, "Screen bin power = " + (int) screenBinPower
						+ ", time = " + brightnessTime);
			}
		}
		power /= 1000; // To seconds
		if (DEBUG)
			Log.i(TAG, "Screen  power is " + power);
		return power;

	}

	public double getRadioUsage(long uSecNow) {// 待机、扫描阶段的功耗吗？
		double power = 0;
		final int BINS = 2;// SignalStrength.NUM_SIGNAL_STRENGTH_BINS;
		long signalTimeMs = 0;
		for (int i = 0; i < BINS; i++) {
			long strengthTimeMs = mStats.getPhoneSignalStrengthTime(i, uSecNow,
					mStatsType) / 1000;
			power += strengthTimeMs
					/ 1000
					* mPowerProfile.getAveragePower(
							PowerProfile.POWER_RADIO_ON, i);
			signalTimeMs += strengthTimeMs;
		}
		long scanningTimeMs = mStats.getPhoneSignalScanningTime(uSecNow,
				mStatsType) / 1000;
		power += scanningTimeMs
				/ 1000
				* mPowerProfile
						.getAveragePower(PowerProfile.POWER_RADIO_SCANNING);
		if (DEBUG)
			Log.i(TAG, "Radio power is " + power);
		return power;

	}

	public double getWiFiUsage(long uSecNow) {// 获取WIFI的功耗
		long onTimeMs = mStats.getWifiOnTime(uSecNow, mStatsType) / 1000;
		long runningTimeMs = mStats.getGlobalWifiRunningTime(uSecNow,
				mStatsType) / 1000;
		if (DEBUG)
			Log.i(TAG, "WIFI runningTime=" + runningTimeMs
					+ " app runningTime=" + mAppWifiRunning);
		runningTimeMs -= mAppWifiRunning;
		if (runningTimeMs < 0)
			runningTimeMs = 0;
		double wifiPower = (onTimeMs * 0 /* TODO */
				* mPowerProfile.getAveragePower(PowerProfile.POWER_WIFI_ON) + runningTimeMs
				* mPowerProfile.getAveragePower(PowerProfile.POWER_WIFI_ACTIVE)) / 1000;
		if (DEBUG)
			Log.i(TAG, "WIFI power=" + wifiPower + " from procs=" + mWifiPower);
		return wifiPower;

	}

	public double getCPUIdleUsage(long uSecNow) {// 系统CPU空闲时的耗电量
		long idleTimeMs = (uSecNow - mStats
				.getScreenOnTime(uSecNow, mStatsType)) / 1000;
		double idlePower = (idleTimeMs * mPowerProfile
				.getAveragePower(PowerProfile.POWER_CPU_IDLE)) / 1000;
		if (DEBUG)
			Log.i(TAG, "idle power  is " + idlePower);
		return idlePower;
	}

	public double getBluetoothUsage(long uSecNow) {// 蓝牙的耗电量
		long btOnTimeMs = mStats.getBluetoothOnTime(uSecNow, mStatsType) / 1000;
		double btPower = (btOnTimeMs * mPowerProfile
				.getAveragePower(PowerProfile.POWER_BLUETOOTH_ON)) / 1000;
		int btPingCount = mStats.getBluetoothPingCount();
		btPower += (btPingCount * mPowerProfile
				.getAveragePower(PowerProfile.POWER_BLUETOOTH_AT_CMD)) / 1000;
		if (DEBUG)
			Log.i(TAG, "blueTooth Usage is " + btPower);
		return btPower;
	}

	public long getSectime() {// 获取上次充电到现在的时间
		final int which = mStatsType;
		long uSecTime = SystemClock.elapsedRealtime() * 1000;
		final long uSecNow = mStats.computeBatteryRealtime(uSecTime, which);
		final long timeSinceUnplugged = uSecNow;
		if (DEBUG) {
			Log.i(TAG, "Uptime since last unplugged = "
					+ (timeSinceUnplugged / 1000));
		}
		return uSecNow;
	}

	private double getAverageDataCost() {
		final long WIFI_BPS = 1000000; // TODO: Extract average bit rates from
										// system
		final long MOBILE_BPS = 200000; // TODO: Extract average bit rates from
										// system
		final double WIFI_POWER = mPowerProfile
				.getAveragePower(PowerProfile.POWER_WIFI_ACTIVE) / 3600;
		final double MOBILE_POWER = mPowerProfile
				.getAveragePower(PowerProfile.POWER_RADIO_ACTIVE) / 3600;
		// mPowerProfile.getAveragePower();
		final long mobileData = mStats.getMobileTcpBytesReceived(mStatsType)
				+ mStats.getMobileTcpBytesSent(mStatsType);
		final long wifiData = mStats.getTotalTcpBytesReceived(mStatsType)
				+ mStats.getTotalTcpBytesSent(mStatsType) - mobileData;
		final long radioDataUptimeMs = mStats.getRadioDataUptime() / 1000;
		final long mobileBps = radioDataUptimeMs != 0 ? mobileData * 8 * 1000
				/ radioDataUptimeMs : MOBILE_BPS;

		double mobileCostPerByte = MOBILE_POWER / (mobileBps / 8);
		double wifiCostPerByte = WIFI_POWER / (WIFI_BPS / 8);
		if (wifiData + mobileData != 0) {
			return (mobileCostPerByte * mobileData + wifiCostPerByte * wifiData)
					/ (mobileData + wifiData);
		} else {
			return 0;
		}
	}

	public void processMiscUsage() {
		final int which = mStatsType;
		long uSecTime = SystemClock.elapsedRealtime() * 1000;
		final long uSecNow = mStats.computeBatteryRealtime(uSecTime, which);
		final long timeSinceUnplugged = uSecNow;
		if (DEBUG) {
			Log.i(TAG, "Uptime since last unplugged = "
					+ (timeSinceUnplugged / 1000));
		}
		// addPhoneUsage(uSecNow);// 通话的耗电量
		// addScreenUsage(uSecNow);
		// addWiFiUsage(uSecNow);
		// addBluetoothUsage(uSecNow);
		// addIdleUsage(uSecNow); // Not including cellular idle power
		// // Don't compute radio usage if it's a wifi-only device
		// addRadioUsage(uSecNow);// 无线射频模块的耗电量
	}

	public void getDevicePowerUnit() {

		double PHONE_ON = mPowerProfile
				.getAveragePower(PowerProfile.POWER_RADIO_ACTIVE);// 通话时的功耗
		double SCREEN_FULL = mPowerProfile
				.getAveragePower(PowerProfile.POWER_SCREEN_FULL);// 屏幕全亮时的功耗
		double SCREEN_ON = mPowerProfile
				.getAveragePower(PowerProfile.POWER_SCREEN_ON);// 屏幕开启的功耗
		double WIFI_ON = mPowerProfile
				.getAveragePower(PowerProfile.POWER_WIFI_ACTIVE);// wifi的开启的功耗
		double MOBILE_POWER = mPowerProfile
				.getAveragePower(PowerProfile.POWER_RADIO_ACTIVE);// 收发移动数据
		double CPU_IDLE = mPowerProfile
				.getAveragePower(PowerProfile.POWER_CPU_IDLE);// CPU两个状态
		double CPU_AWAKE = mPowerProfile
				.getAveragePower(PowerProfile.POWER_CPU_AWAKE);
		double BLUETOOTH_ON = mPowerProfile
				.getAveragePower(PowerProfile.POWER_BLUETOOTH_ON);
		double GPS_ON = mPowerProfile
				.getAveragePower(PowerProfile.POWER_GPS_ON);
		double AUDIO_ON = mPowerProfile
				.getAveragePower(PowerProfile.POWER_AUDIO);
		if (DEBUG)
			Log.i(TAG, CPU_IDLE + "," + CPU_AWAKE + "," + SCREEN_ON + ","
					+ SCREEN_FULL + "," + MOBILE_POWER + "," + AUDIO_ON + ","
					+ WIFI_ON + "," + BLUETOOTH_ON + "," + GPS_ON + ","
					+ PHONE_ON);
	}

	public PowerUsage(Context context) {
		mPowerProfile = new PowerProfile(context);
		mBatteryInfo = IBatteryStats.Stub.asInterface(ServiceManager
				.getService("batteryinfo"));
		try {
			byte[] data = mBatteryInfo.getStatistics();
			Parcel parcel = Parcel.obtain();
			parcel.unmarshall(data, 0, data.length);
			parcel.setDataPosition(0);
			mStats = com.android.internal.os.BatteryStatsImpl.CREATOR
					.createFromParcel(parcel);
			mStats.distributeWorkLocked(BatteryStats.STATS_SINCE_CHARGED);
		} catch (RemoteException e) {
			Log.e("try", "RemoteException:", e);
		}
	}
}
