package com.li.helpclass;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Calendar;
import android.util.Log;

public class ReadWriteFile {
	
    public void writeFile(String str, String path){  
        File file; 
        try {  
        	
            // create a new file  
            file = new File(path);
            if(!file.exists()){
            	file.createNewFile();
            }
            BufferedWriter out = null;  
            try {  
                out = new BufferedWriter(new OutputStreamWriter(  
                        new FileOutputStream(file, true)));  
                out.write(str); 
            } catch (Exception e) {  
                e.printStackTrace();  
            } finally {  
                try {  
                    out.close();  
                } catch (IOException e) {  
                    e.printStackTrace();  
                }  
            }    
            
        } catch (IOException e) {  
            // output the error informatin to logcat 
            Log.d("IOException", e.toString());
        }  
    }  
    
    public String getNowTime(){

   	 Calendar c = Calendar.getInstance();
   	 int year = c.get(Calendar.YEAR);
   	 int month = c.get(Calendar.MONTH)+1;
   	 String m = formatDate(month);
   	 
   	 int day = c.get(Calendar.DAY_OF_MONTH);
   	 String d = formatDate(day);
   	 int hour = c.get(Calendar.HOUR_OF_DAY);
   	 String h = formatDate(hour);
   	 
   	 int minute = c.get(Calendar.MINUTE);
   	 String m1 = formatDate(minute);
   	 int second = c.get(Calendar.SECOND);
   	 String s = formatDate(second);
   	 //return year + "-" +m +"-" +d +" " +h +":"+m1+":"+s;
   	 return h+":"+m1+":"+s;
    }
    
    public String formatDate(int data){
   	 String d;
   	 if(data<10){
   		 d = "0" + data;
   	 }else{
   		 d = String.valueOf(data);
   	 }
   	 return d;
    }

}
