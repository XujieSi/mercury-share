package edu.gatech.offloading;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Hashtable;

//import android.util.Log;
import edu.gatech.protocol.Log;


import edu.gatech.protocol.MetaData;
import edu.gatech.util.Utility;


// run on client side
public class ResultReceiver implements Runnable {

	private static final String TAG = "ReturnReceiver";

	private Hashtable<String, ClientOffloadingTask> jobTable = null;
	private Socket socket = null;	

	public ResultReceiver(Socket socket, Hashtable<String, ClientOffloadingTask> _jobTable) {
		this.socket = socket;
		this.jobTable = _jobTable;
		new Thread(this, "ReturnReceiver").start();
	}

	public void run() {

		Log.d(TAG, "New ReturnReciever, Accepted connection : " + socket);

		try {
			
			long start = System.nanoTime();
			
			InputStream is = socket.getInputStream();

			// read meta data
			byte[] mdata = Utility.readNBytes(is, MetaData.METADATA_LENGTH);
			MetaData meta = new MetaData(new String(mdata));
			Log.d(TAG, meta.getLogString());

			// receive data
			Log.d(TAG, "Receiving  " + meta.getContentLength() + " bytes ...");
			byte[] mybytearray = Utility.readNBytes(is, meta.getContentLength());

			long end = System.nanoTime();
			is.close();
			socket.close();
			
			Log.d(TAG, "Transfer took: " + Long.toString(end - start));
			Utility.logTime(meta.getID(), TAG+"-begin" + "\t" + start, meta.getContentLength());
			Utility.logTime(meta.getID(), TAG+"-end", end);
			

			Log.d( TAG, "Successful result download");

			if (mybytearray == null) {
				Log.d(TAG, "Null mybytearray");
			}

			ClientOffloadingTask clientObj = jobTable.get(meta.getID() );

			if (clientObj == null) {
				Log.d(TAG, "Null clientObj, cannot receive result");
			} else {
				clientObj.receiveResult(mybytearray);
			}

			// ClientObj.jobId = ClientObj.jobId + 1;
			// Log.d (TAG, "Set jobId to " + ClientObj.jobId);
			// Log.d (TAG1, "Set jobId to " + ClientObj.jobId);
			// if downloading is failed:
			// clientObj.setClientState(ClientObj.PING_RESULT_SERVER);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
