package edu.gatech.networkMonitor;

import java.io.File;
import java.util.Timer;

import android.app.Activity;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
public class MainPanel extends Activity implements OnClickListener{
	private File root = Environment.getExternalStorageDirectory();
	static final int DATASIZE = 1460;
	static int interval = 10*1000; 
		
	boolean mStarted = false;
	static String mip;
	
	WifiMonitor mWifiMonitor;
	EditText mIPEdit;
	Button	 mButtonStart;
	TextView mInfo;
	EditText mIntervalEdit;
	
	int mSession;
	long mDate;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) { 
    	Log.d ("NM", "Starting Network monitor");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mIPEdit = (EditText)findViewById(R.id.IPText);
        mIntervalEdit = (EditText)findViewById(R.id.editInterval);
        mButtonStart  = (Button)findViewById(R.id.buttonStart);
        mButtonStart.setOnClickListener(this);
        mInfo = (TextView)findViewById(R.id.textLog);
        
        mSession = (int)(System.currentTimeMillis()/(1000*60*15)%96);
		mDate = System.currentTimeMillis()/(1000*3600*24)%365;
		//this.registerReceiver(mWifiMonitor, new IntentFilter(WifiManager.RSSI_CHANGED_ACTION));
		//this.registerReceiver(mWifiMonitor, new IntentFilter(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION));
    }
    

    public void onDestroy(){
    	//this.unregisterReceiver(mWifiMonitor);
    	super.onDestroy();
    }
    Timer timer = new Timer();
    public void onClick(View view) {
    	if(!view.equals(mButtonStart))
    		return;
    	mStarted = !mStarted;

    	if(mStarted){
    		String timestamp = Long.toString(System.currentTimeMillis());
    		File fromWifi = new File (root + File.separator + "cirrus" + 
    				File.separator +  "wifi_onoff.txt");
    		
    		File toWifi = new File (root + File.separator + "cirrus" + 
    				File.separator +  "wifi_onoff" + 
    				timestamp + ".txt");
    		fromWifi.renameTo(toWifi);
    		
    		File fromSig = new File (root + File.separator + "cirrus" + 
    				File.separator +  "signaldown.txt");
    		
    		File toSig = new File (root + File.separator + "cirrus" + 
    				File.separator +  "signaldown" + 
    				timestamp + ".txt");
    		fromSig.renameTo(toSig);
    		
			File fromHandoff = new File (root + File.separator + "cirrus" + 
					File.separator +  "handoff.txt");
			
			File toHandoff = new File (root + File.separator + "cirrus" + 
								File.separator +  "handoff" + 
									timestamp + ".txt");
			fromHandoff.renameTo(toHandoff);			
    		
    		mButtonStart.setText("Stop");
    		mWifiMonitor = new WifiMonitor(this);
    		Pinger pinger = new Pinger(this);
    		timer.scheduleAtFixedRate( mWifiMonitor,
		 			  0, 
		 			  200); 
    		timer.scheduleAtFixedRate( pinger,
		 			  0, 
		 			  500);
    		interval = Integer.parseInt(mIntervalEdit.getText().toString())*1000;
    		mip = mIPEdit.getText().toString();
    		NetMeasurer measurer = new NetMeasurer(interval, this);
			measurer.start();
    	}
    	else{
    		mButtonStart.setText("Start");
    		timer.cancel();
    	}
    }
    public void postResult(String s){
		final String msg = s;
		this.mInfo.post(new Runnable(){
			public void run() {
				mInfo.setText(msg);
			}
		});
	}
    
    public boolean isConnected(){
    	return mWifiMonitor.connected;
    }
    
    public int getRssi(){
    	return mWifiMonitor.getRssi();
    }
}