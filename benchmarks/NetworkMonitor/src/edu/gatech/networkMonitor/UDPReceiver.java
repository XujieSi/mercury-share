package edu.gatech.networkMonitor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;

import android.os.Environment;

public class UDPReceiver extends Thread {
	private File root = Environment.getExternalStorageDirectory();
	static final int interval = MainPanel.interval;
	long mtime;
	long date;
	long sid;
	long sampleid;
	double lat;
	double longt;
	DatagramSocket msocket;
	int mrssi;
	double mspeed;
	
	byte[] receiverBuffer = new byte[UDPSender.UDPDATASIZE];
	
	boolean started = false;
	
	long startTime;
	long lastTime;
	int count = 0;
	String apName;

	MainPanel mPanel;
	
	
	public UDPReceiver(DatagramSocket socket, long time, int session, int sample, 
			int rssi, MainPanel panel, String ap){
		this(socket,  time,  session,  sample,  rssi,  panel);
		apName = ap;
	}
	public UDPReceiver(DatagramSocket socket, long time, int session, int sample, 
			int rssi, MainPanel panel){
		msocket = socket;
		mtime = time;
		sid = session;
		sampleid = sample;
		date = panel.mDate;
		mrssi = rssi;
		mPanel = panel;
	}
	
	@Override
	public void run(){
		while(true){
		try{
			DatagramPacket packet = new DatagramPacket(receiverBuffer, UDPSender.UDPDATASIZE);
			try{
			msocket.receive(packet);
			}catch(SocketTimeoutException e){
				record();
				return;
			}
			String data = new String(packet.getData());
			//Log.d("SignalMap", "udp packet: "+data.substring(0, 20));
			if(!started){
				started = true;
				startTime = System.currentTimeMillis();
			}
			
			//received packet
			long currentTime = System.currentTimeMillis();
			if(currentTime - startTime > 5000){
				//last session doesn't finish.
				//record it
				record();
				return;
			}
			
			//update info
			lastTime = currentTime;

			if(data.startsWith("end")){
				//end of session
				record();
				return;
			}
			count++;
		}
		catch(Exception e){}
		}

	}
	
	public void record(){
		
		
		double bandwidth = count*UDPSender.UDPDATASIZE*1.0/(lastTime - startTime)*1000.0/1024.0;
		mrssi = mPanel.mWifiMonitor.getAvgRssi();
//		File f = new File("/sdcard/cirrus/signaldown-"+date+"-"+sid+".txt");
		File f = new File (root + File.separator + "cirrus" + 
										File.separator +  "signaldown.txt");
		try{
			BufferedWriter writer = new BufferedWriter(new FileWriter(f,true));
			writer.append(sampleid+"\t"+mtime+"\t"+bandwidth+"\t"
					+count+"\t"+mrssi+"\t"+"\n");
			writer.flush();
			writer.close();
			msocket.close();
		}catch(Exception e){
			
		}
	}
}

