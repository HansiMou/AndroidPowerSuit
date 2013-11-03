package com.li.helpclass;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

public class ExternalStorage {
	private final String TAG="ExternalStorage";
	private long blocSize;
	private long totalBlocks;
	private long availaBlock;
	public long getBlockSize(){
		getExternalStorageInfo();
		return blocSize;
	}
	
	public long getTotalBlock(){
		getExternalStorageInfo();
		return totalBlocks;
	}
	
	public long getAvailableBlock(){
		getExternalStorageInfo();
		return availaBlock;
	}
	
	public String getBlockUsage(){
		getExternalStorageInfo();
		NumberFormat numberFormat = NumberFormat.getInstance();
		numberFormat.setMaximumFractionDigits(2);
		String result = numberFormat.format((float)(totalBlocks-availaBlock)/(float)totalBlocks*100);
		return result;
	}
	
	public void getExternalStorageInfo() {
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			// ȡ��sdcard�ļ�·��
			File path = Environment.getExternalStorageDirectory(); // "mnt/sdcard"
			path = new File("mnt/sdcard");
//			Log.i(TAG, "======path========" + path);

			StatFs statfs = new StatFs(path.getPath());
			// ��ȡblock��SIZE
			 blocSize = statfs.getBlockSize();
			// ��ȡBLOCK����
			 totalBlocks = statfs.getBlockCount();
			// ���е�Block������
			 availaBlock = statfs.getAvailableBlocks();
			// �����ܿռ��С�Ϳ��еĿռ��С
			 String[] total = filesize(totalBlocks * blocSize);
			 String[] available = filesize(availaBlock * blocSize);
			
//	 Log.i("TAG","Block����:"+totalBlocks+","+"����Block����:"+availaBlock+","+"SD����С��"+total[0]+total[1]+","+"SD�����ô�С��"+available[0]+available[1]);

		}
	}

	String[] filesize(long size) {
		String str = "";
		//��λ����
		if (size >= 1024) {
			str = "KB";
			size /= 1024;
			if (size >= 1024) {
				str = "MB";
				size /= 1024;
				if(size >= 1024){
					str = "GB";
				}
			}
		}
		//������
		DecimalFormat formatter = new DecimalFormat();
		formatter.setGroupingSize(3);
		String result[] = new String[2];
		result[0] = formatter.format(size);
		result[1] = str;
		return result;
	}
}
