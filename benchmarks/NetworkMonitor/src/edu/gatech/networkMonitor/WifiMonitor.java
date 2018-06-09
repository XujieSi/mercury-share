package edu.gatech.networkMonitor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.TimerTask;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

public class WifiMonitor extends TimerTask{
	static boolean connected = false;
	static int rssi = -200;
	float avgRssi = 0;
	int count = 0;
	String apMac = "";
	WifiManager wm;
	MainPanel mPanel;
	public WifiMonitor(MainPanel panel){
		mPanel = panel;
		wm = (WifiManager)panel.getSystemService(Context.WIFI_SERVICE);
	}
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		rssi = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI,-200);
		if(action.equals(WifiManager.RSSI_CHANGED_ACTION)){
			if(!connected){
				if(rssi > -200){
					long time = System.currentTimeMillis();
					connected = true;
					synchronized (this) {
			   			this.notify();
			   		}
					record(time,1);
				}
			}
			else if(rssi <= -100){
				connected = false;
				record(System.currentTimeMillis(), 0);
			}
		}
		else if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)){
			if (intent.getBooleanExtra(
                    WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)) {
				if(!connected){
					connected = true;
					long time = System.currentTimeMillis();
					synchronized (this) {
			   			this.notify();
			   		}
					record(time,1);
				}
			}
			else{
				if(connected){
					connected = false;
					record(System.currentTimeMillis(), 0);
				}
			}
		}
	}
	public int getRssi(){
		return rssi;
	}

	public void record(long time, int on){
		File f = new File("/sdcard/cirrus/wifi_onoff.txt");
		try{
			BufferedWriter writer = new BufferedWriter(new FileWriter(f,true));
			writer.append(time+"\t"+on+"\n");
			writer.flush();
			writer.close();
		}catch(Exception e){
			
		}
	}
	public void recordHandoff(long time, String apName){
		File f = new File("/sdcard/cirrus/handoff.txt");
		try {
		BufferedWriter writer = new BufferedWriter(new FileWriter(f,true));
		writer.append(time+"\t"+apName+"\n");
		writer.flush();
		writer.close();
		} catch(Exception e){

		}
	}
		public void run() {

		WifiInfo wi = wm.getConnectionInfo();
		//mRssi = mPanel.getRssi();
		rssi = wi.getRssi();
		String apName = wi.getBSSID();
		if(apMac == null ) {
			if(apName != null) {
				apMac = apName;
				recordHandoff(System.currentTimeMillis(),apMac);
			}
		} else if (apName == null){
			apMac = apName;
		} else if(apMac.compareTo(apName) != 0) {
			apMac = apName;
			recordHandoff(System.currentTimeMillis(),apMac);
		}
	    
	    
	    
	    if(apName == null)
	    	rssi = -200;
	    //update average rssi
	    avgRssi = (count*avgRssi + rssi)/(count+1);
	    count++;
	    //check connectivity
		if(!connected){
			if(rssi > -100){
				long time = System.currentTimeMillis();
				connected = true;
				synchronized (this) {
		   			this.notify();
		   		}
				record(time,1);
			}
		}
		else if(rssi < -100){
			connected = false;
			record(System.currentTimeMillis(), 0);
		}
	}
	
	public void startRecordAverageRssi(){
		count = 1;
		avgRssi = rssi;
	}
	public int getAvgRssi(){
		return (int)avgRssi;
	}
}
