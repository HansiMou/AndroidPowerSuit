package com.li.helpclass;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.NumberFormat;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.TextView;

public class ReadSystemMemory {
	public static final int REFRESH = 0x000001;
	private final String  TAG="MemoryUsage";
	public TextView tv = null;
	long total = 0;
	long idle = 0;
	double usage = 0;
	ActivityManager am;
	Context context;

	public ReadSystemMemory(ActivityManager am, Context context) {
		this.am = am;
		this.context = context;
	}

	public void CPULoad() {
		readCPUUsage();
	}

	public void readCPUUsage() {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream("/proc/stat")), 1000);
			String load = reader.readLine();
			reader.close();

			String[] toks = load.split(" ");

			long currTotal = Long.parseLong(toks[2]) + Long.parseLong(toks[3])
					+ Long.parseLong(toks[4]);
			long currIdle = Long.parseLong(toks[5]);

			this.usage = (currTotal - total) * 100.0f
					/ (currTotal - total + currIdle - idle);
			this.total = currTotal;
			this.idle = currIdle;
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public double getCPUUsage() {
		readCPUUsage();
		return usage;
	}
	/*可用内存的大小*/
	public String getAvailMemory() {
		MemoryInfo mi = new MemoryInfo();//还有这么一个对象
		am.getMemoryInfo(mi);
		return String.valueOf(getNumFromString(Formatter.formatFileSize(context, mi.availMem)));// 将获取的内存大小规格化

	}
	/*读取系统中的总内存*/
	public String getTotalMemory() {
		String str1 = "/proc/meminfo";// 系统内存信息文件
		String str2;
		String[] arrayOfString;
		long initial_memory = 0;
		try {
			FileReader localFileReader = new FileReader(str1);
			BufferedReader localBufferedReader = new BufferedReader(
			localFileReader, 8192);
			str2 = localBufferedReader.readLine();// 读取meminfo第一行，系统总内存大小
			arrayOfString = str2.split("\\s+");
			for (String num : arrayOfString) {
			//	Log.i(str2, num + "\t");//判断所读出数据的大小
			}

			initial_memory = Integer.valueOf(arrayOfString[1]).intValue() * 1024;// 获得系统总内存，单位是KB，乘以1024转换为Byte
			localBufferedReader.close();
		} catch (IOException e) {
			
		}

		return String.valueOf(getNumFromString(Formatter.formatFileSize(context, initial_memory)));// Byte转换为KB或者MB，内存大小规格化
	}
	/*获取字符串中的数字*/
	public  int getNumFromString(String temp){
		String str2 = "";
		if (temp != null && !"".equals(temp)) {
			for (int i = 0; i < temp.length(); i++) {
				if (temp.charAt(i) >= 48 && temp.charAt(i) <= 57) {
					str2 += temp.charAt(i);
				}
			}		
		}
		return Integer.parseInt(str2);
	}
	/*计算内存的利用率*/
	public String  getMemoryUsage(){
		int totalMem=getNumFromString(getTotalMemory());//处理系统总内存
		int availableMem=getNumFromString(getAvailMemory());//处理系统可用内存
//		Log.i(TAG," "+totalMem+" "+availableMem);

		NumberFormat numberFormat = NumberFormat.getInstance();
		numberFormat.setMaximumFractionDigits(2);

		String result = numberFormat.format((float)(totalMem-availableMem)/(float)totalMem*100);
		return result; //(totalMem-availableMem)/totalMem;
		
	}
}
