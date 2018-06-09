package edu.gatech.offloading;


import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import edu.gatech.protocol.MetaData;
import edu.gatech.protocol.Utility;

//import android.util.Log;
import edu.gatech.protocol.Log;


// run on server side
public class RawDataSender implements Runnable {
	
	private byte[] mybytearray = null;
	private String remoteIP = null;
	private int remotePort;
	private String logID;
	
	private static final String TAG = "RawDataSender";

	public static int SEND_SUCCESS = 0;
	public static int SEND_FAILED = -1;

	public RawDataSender(String _remoteIP, int _remotePort, byte[] bufferToSend, MetaData meta) {
		this.remoteIP = _remoteIP;
		this.remotePort = _remotePort;
		
		if(meta.getContentLength() != bufferToSend.length){
			Log.d(TAG, "meta info is not consistent with bufferToSender");
			meta.setContentLength(bufferToSend.length);
		}
		
		byte[] metadata = meta.getBytes();

		Log.d(TAG, "metadata is: " + new String(metadata) + "and its length is "
				+ metadata.length + ", bufferToSend length = " + bufferToSend.length);

		logID = meta.getID();
		
		mybytearray = new byte[bufferToSend.length + metadata.length];
		System.arraycopy(metadata, 0, mybytearray, 0, metadata.length);
		System.arraycopy(bufferToSend, 0, mybytearray, metadata.length,
				bufferToSend.length);

		Log.d(TAG, "Overall sending length: " + mybytearray.length);

		new Thread(this, "RawDataSender").start();
	}

	public void run() {
		Log.d(TAG, "in file send, remoteIP="+remoteIP + ", remotePort=" + remotePort);
		
		try {
			long start = System.nanoTime();
			
			Socket socket = new Socket(remoteIP, remotePort);
			OutputStream os = socket.getOutputStream();
			
			os.write(mybytearray);
			os.flush();
			os.close();
			socket.close();
			
			long end = System.nanoTime();
			
			Log.d(TAG, "Sending result is done successfully, duration: " + (end - start));
			Utility.logTime(logID, TAG+"-begin" + "\t" + start, mybytearray.length);
			Utility.logTime(logID, TAG+"-end", end);
			
		} catch (IOException e) {
			Log.d(TAG, "Returning fail from FIleSender");
			e.printStackTrace();
		}
		
	}
}
