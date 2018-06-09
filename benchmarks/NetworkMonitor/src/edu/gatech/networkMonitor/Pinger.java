package edu.gatech.networkMonitor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.InetAddress;
import java.util.TimerTask;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

public class Pinger extends TimerTask{
	int[] tests = new int[4]; 
	MainPanel panel;
	WifiManager wm;
	int count = 0;
	boolean handoff = false;
	long handoffStartTime;
	String mac;
	public Pinger(MainPanel mp){
		panel = mp;
		wm = (WifiManager)panel.getSystemService(Context.WIFI_SERVICE);
	}

	public void recordHandoff(long time, String apName){
		File f = new File("/sdcard/cirrus/handoff.txt");
		try{
			BufferedWriter writer = new BufferedWriter(new FileWriter(f,true));
			writer.append(time+"\t"+apName+"\n");
			writer.flush();
			writer.close();
		}catch(Exception e){
			
		}
	}
	
	/**
	 * 0: false positive
	 * 1: correct
	 * 2: missed 
	 * 4: disconnect
	 */
	public void run(){
		try{
			InetAddress ipaddress =  InetAddress.getByName(panel.mip);
			if(ipaddress.isReachable(200)){   
				tests[count] = 0; 
			}
			else
				tests[count] = 1;
			count = (count + 1)%4;
			if(handoff){
				if(tests[0]+tests[1]+tests[2]+tests[3] <= 0)
				{
					//finish
					handoff = false;
					int detect = 1;
					String newmac = wm.getConnectionInfo().getBSSID();
					if(mac == null || newmac == null){
						if(mac == newmac)
							detect = 0;
					}
					else if(mac.compareTo(newmac) == 0)
						detect = 0;
						
					recordHandoff(handoffStartTime, System.currentTimeMillis()+
							"\t"+detect);
				}
				if(System.currentTimeMillis() - handoffStartTime > 20000
						&& wm.getConnectionInfo().getBSSID() == null)
					recordHandoff(handoffStartTime, System.currentTimeMillis()+
							"\t"+4);
				handoff =false;
			}
			else{
				if(tests[0]+tests[1]+tests[2]+tests[3] >= 3)
				{
					//handoff
					handoff = true;
					mac = wm.getConnectionInfo().getBSSID();
					handoffStartTime = System.currentTimeMillis();
				}
			}
		}catch(Exception e){}
	}
}
