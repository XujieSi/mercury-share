package edu.gatech.offloading;


//package edu.gatech.offloadingservice;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;

//import edu.gatech.filehandler.*;
import edu.gatech.jobinstance.JobInstance;
import edu.gatech.protocol.Utility;


//import android.content.Context;
//import android.os.NetworkOnMainThreadException;
//import android.util.Log;
import edu.gatech.protocol.Log;


public class OffloadExecutionServer implements Runnable {
	public static final int serverListenPort = 2345;
	//public static final String appAPK = "/sdcard/DCIM/IntermitentConnectivityMult.apk";
	private static final String TAG = "OffloadExecutionServer"; // edu.gatech.main.MainActivity.TAG;
    private ServerSocket execServerSocket = null;
    private Hashtable<String, JobInstance> jobTable;
    //private Context context;
    
    private static OffloadExecutionServer  uniqServer= null;
    private static String lastClientIP = null; // this is a simple hack, does not work for more than one client
    
    
    //special constructor for client side
	public OffloadExecutionServer(Hashtable<String, JobInstance> _jobTable, /*Context _context,*/ String remoteIP) {
		
		if(uniqServer != null){
			Log.d(TAG, "There is another offloading execution server running!! Cannot start any more ");
			return;
		}
		uniqServer = this;
		this.jobTable = _jobTable;
		//this.context = _context;
		
		lastClientIP = remoteIP; // Only for client side
		Log.d (TAG, "OffloadExecutionServer(client) starting ... ");
		
		try {
            execServerSocket = new ServerSocket(serverListenPort);
            Log.d (TAG, "OffloadExecutionServer(client) started on port " + serverListenPort);
        } catch (IOException e) {
            Log.d (TAG, "OffloadExecutionServer(client) could not listen on port: " + serverListenPort);
            System.exit(-1);
        }
		
		new Thread(this, "OffloadExecutionServer(client)").start();
	}
   
	public OffloadExecutionServer(Hashtable<String, JobInstance> _jobTable) { //, Context _context) {
		
		if(uniqServer != null){
			Log.d(TAG, "There is another offloading execution server running!! Cannot start any more ");
			return;
		}
		uniqServer = this;
		this.jobTable = _jobTable;
		//this.context = _context;
		
		Log.d (TAG, "OffloadExecutionServer starting ... ");
		
		try {
            execServerSocket = new ServerSocket(serverListenPort);
            Log.d (TAG, "OffloadExecutionServer started on port " + serverListenPort);
        } catch (IOException e) {
            Log.d (TAG, "OffloadExecutionServer could not listen on port: " + serverListenPort);
            System.exit(-1);
        }
		
		new Thread(this, "OffloadExecutionServer").start();
	}
	
	//@Override
	public void run() {
        while (true){
		    try {
		    	Socket clientRequest = execServerSocket.accept();
		    	lastClientIP = Utility.socketToIP(clientRequest);
		    	new OffloadExecutionThread(clientRequest, jobTable); //, context);
			} catch (IOException e) {
				Log.d (TAG, "IO Ex in OffloadExecutionServer accept");
				e.printStackTrace();
			}
        }
	}
	
	
	public static String whereAmIFrom(){
		if(lastClientIP==null){
			Log.d(TAG, "Got request while lastClientIP is still empty");
			return "You must be a moneky!"; // I cannot find what place you are from!
		}
		
		return lastClientIP;
	}

}
