package edu.gatech.networkMonitor;


import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

public class NetMeasurer extends Thread {
	//measure info
	int mMeasureInterval;
	MainPanel mPanel;
	
	int mSession;
	int mSample;
	
	long mStartTime = 0;
	int mRssi;
	String apName;
	
	public NetMeasurer(int interval,  MainPanel panel){
		mMeasureInterval = interval;
		mPanel = panel;

		mSession = (int)(System.currentTimeMillis()/(1000*60*15)%96);
		mSample =0;
	}
	
	@Override
	public void run(){
		
		while(mPanel.mStarted){
			//current disconnected
			if(!mPanel.isConnected()){
				mPanel.postResult("Disconnected");
				long current = System.currentTimeMillis();
				try{
					synchronized (mPanel.mWifiMonitor) {
			   			mPanel.mWifiMonitor.wait();
			   		}
				}catch(Exception e){
					mPanel.postResult("Exception for wait :"+e.getMessage());
				}
				mPanel.postResult("wait for"+(System.currentTimeMillis()-current));
			}
			
			mStartTime = System.currentTimeMillis();
	    	WifiMeasure();
			UDPMeasure();
			mPanel.postResult(apName+"\t"+mPanel.getRssi()+"\n");
			try{
				long interval = mStartTime + mMeasureInterval - System.currentTimeMillis();
				if(interval > 0)
					sleep(interval);
			}catch(Exception e){}
		}
		
	}
	
	private void WifiMeasure(){
		WifiManager wm = (WifiManager)mPanel.getSystemService(Context.WIFI_SERVICE);
	    WifiInfo wi = wm.getConnectionInfo();
	    //mRssi = mPanel.getRssi();
	    mRssi = wi.getRssi();
	    apName = wi.getBSSID();
		
	}
	
	private void UDPMeasure(){
    	UDPSender sender;
    	mPanel.mWifiMonitor.startRecordAverageRssi();
    	sender = new UDPSender(mRssi, mSession,mSample,apName);
    	UDPReceiver rcv;
    	rcv= new UDPReceiver(sender.getSocket(), mStartTime, mSession,mSample,
	    		mRssi,mPanel,apName);
    	sender.start();
    	rcv.start();
    	mSample++;
	}
}
