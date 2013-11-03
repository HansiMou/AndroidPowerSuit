package com.li.helpclass;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import android.util.Log;

public class CpuInfo{
    final public String CPUPATH = "/mnt/sdcard/cpu.txt";
    private final String MEMPATH="/proc/meminfo";
    final String TAG="ModuleStated";
    private StringBuffer bsstr=null;
    private String cpuInfo1 = "";
    private long idle1=-1,cpu1=-1;
    private long min,relong;
    private static int corenum=0;
    
    public CpuInfo()
    {
    	min=getMinCpuFreq();
		relong=getMaxCpuFreq()-min;
    }
    public double getFreqRatio()
    {    	
		long now=getCurCpuFreq();
		double cur=(double)(now-min)/relong;
		return cur;
    }
    
	public double getCpuUsage() {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(
					"/proc/stat"));
			// RandomAccessFile reader = new RandomAccessFile("/proc/stat",
			// "r");
			String load = reader.readLine();
			String temp = null;
			// Log.i("未切割",load);
			String[] toks = load.split(" ");
			// for(int i = 0;i<toks.length;i++)temp=temp+" "+toks[i];
			// Log.i("切割后重组",temp);
			// Log.i("切割后直接访问","零"+toks[0]+"一"+toks[1]+"二"+toks[2]+"三"+toks[3]+"四"+toks[4]+"五"+toks[5]+"六"+toks[6]+"七"+toks[7]+"八"+toks[8]);
			long idle2 = Long.parseLong(toks[5]);
			long cpu2 = Long.parseLong(toks[2]) + Long.parseLong(toks[3])
					+ Long.parseLong(toks[4]) + Long.parseLong(toks[6])
					+ Long.parseLong(toks[7]) + Long.parseLong(toks[8]);
			reader.close();
			if ((cpu1 + idle1) < 0) {
				cpu1 = cpu2;
				idle1 = idle2;
				return 0;
			} else {
				float f = (float) (cpu2 - cpu1)
						/ ((cpu2 + idle2) - (cpu1 + idle1));
				cpu1 = cpu2;
				idle1 = idle2;
				if (f < 0){f = 100 + f;}
				else if(f>100){f=100;}
				return f;
			}
		} catch (IOException ex) {
		}
		return 0;
	}
    
	public double getMemoryUsage() {//算内存利用率
		int i = 0;
		File file = new File(MEMPATH);
		long memTotal=0;
		long memFree=0;
		ArrayList<String> list = new ArrayList();
		double[] nums = null;
		try {
			BufferedReader bw = new BufferedReader(new FileReader(file));
			String line = null;
			while ((line = bw.readLine()) != null&& i<=2) {
				list.add(line);
				i++;
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}				
		memTotal=getIntFromString(String.valueOf(list.get(0)));
		memFree=getIntFromString(String.valueOf(list.get(1)));
		//Log.i("ModuleStated",memTotal+" "+memFree);
		//System.out.println(memTotal+" "+memFree);
		return (float)(memTotal-memFree)/(float)memTotal;
	}
	private long getIntFromString(String temp){//获取String中的数字
		  String d ="" ;
		  char lines[]=temp.toCharArray();
		  for(int i=0; i<lines.length; i++)
		  {
		   if (Character.isDigit(lines [i]))
		   {
		    d = d + lines [i];
		   }
		  }
		  return Long.parseLong(d);
	}

	public long getCurCpuFreq()
	{
         String result = "N/A";
         long rr=0;
         corenum = 0;
         try {
        	 for(int i = 0; i<=3; i++){
        		 File a = new File("/sys/devices/system/cpu/cpu"+i+"/cpufreq");
	        	 if(a.exists()){
	        		 FileReader fr = new FileReader(
	                         "/sys/devices/system/cpu/cpu"+i+"/cpufreq/scaling_cur_freq");
			         BufferedReader br = new BufferedReader(fr);
			         String text = br.readLine();
			         result = text.trim();
			         rr=rr+Long.parseLong(result);
			         
			         corenum++;
	        	 }
        	 }
        	 
                 
         }catch (IOException e) {
                 e.printStackTrace();
         }
         return rr;
    }	 
	
	public int getCurCpuCoreNum(){
		return corenum;
	}
	
	public long getMaxCpuFreq() 
	{
	     String result = "";
	     ProcessBuilder cmd;
	     try {
	        String[] args = { "/system/bin/cat", "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq" };
	        cmd = new ProcessBuilder(args);
	        java.lang.Process process = cmd.start();
	        InputStream in = process.getInputStream();
	        byte[] re = new byte[24];
	        while (in.read(re) != -1) {
	           result = result + new String(re);
	        }
	        in.close(); 
	     }catch (IOException ex) {
	        	ex.printStackTrace();
	        	result = "N/A";
	     }
	     return Long.parseLong(result.trim());
	}
		
	public long getMinCpuFreq() 
	{
	     String result = "";
	     ProcessBuilder cmd;
	     try {
	         String[] args = { "/system/bin/cat",
	                           "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq" };
	         cmd = new ProcessBuilder(args);
	         java.lang.Process process = cmd.start();
	         InputStream in = process.getInputStream();
	         byte[] re = new byte[24];
	         while (in.read(re) != -1) {
	               result = result + new String(re);
	         }
	         in.close();
	     } catch (IOException ex) {
	         ex.printStackTrace();
	         result = "N/A";
	     }
	     return Long.parseLong(result.trim());
	}
}
