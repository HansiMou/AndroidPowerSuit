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
	/*�����ڴ�Ĵ�С*/
	public String getAvailMemory() {
		MemoryInfo mi = new MemoryInfo();//������ôһ������
		am.getMemoryInfo(mi);
		return String.valueOf(getNumFromString(Formatter.formatFileSize(context, mi.availMem)));// ����ȡ���ڴ��С���

	}
	/*��ȡϵͳ�е����ڴ�*/
	public String getTotalMemory() {
		String str1 = "/proc/meminfo";// ϵͳ�ڴ���Ϣ�ļ�
		String str2;
		String[] arrayOfString;
		long initial_memory = 0;
		try {
			FileReader localFileReader = new FileReader(str1);
			BufferedReader localBufferedReader = new BufferedReader(
			localFileReader, 8192);
			str2 = localBufferedReader.readLine();// ��ȡmeminfo��һ�У�ϵͳ���ڴ��С
			arrayOfString = str2.split("\\s+");
			for (String num : arrayOfString) {
			//	Log.i(str2, num + "\t");//�ж����������ݵĴ�С
			}

			initial_memory = Integer.valueOf(arrayOfString[1]).intValue() * 1024;// ���ϵͳ���ڴ棬��λ��KB������1024ת��ΪByte
			localBufferedReader.close();
		} catch (IOException e) {
			
		}

		return String.valueOf(getNumFromString(Formatter.formatFileSize(context, initial_memory)));// Byteת��ΪKB����MB���ڴ��С���
	}
	/*��ȡ�ַ����е�����*/
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
	/*�����ڴ��������*/
	public String  getMemoryUsage(){
		int totalMem=getNumFromString(getTotalMemory());//����ϵͳ���ڴ�
		int availableMem=getNumFromString(getAvailMemory());//����ϵͳ�����ڴ�
//		Log.i(TAG," "+totalMem+" "+availableMem);

		NumberFormat numberFormat = NumberFormat.getInstance();
		numberFormat.setMaximumFractionDigits(2);

		String result = numberFormat.format((float)(totalMem-availableMem)/(float)totalMem*100);
		return result; //(totalMem-availableMem)/totalMem;
		
	}
}
