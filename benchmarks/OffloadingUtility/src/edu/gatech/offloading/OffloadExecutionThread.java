package edu.gatech.offloading;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Hashtable;

import edu.gatech.jobinstance.JobInstance;
import edu.gatech.protocol.MetaData;

//import android.content.Context;
//import android.util.Log;
import edu.gatech.protocol.Log;
import edu.gatech.util.Utility;



public class OffloadExecutionThread implements Runnable {
	private static final String TAG = "OffloadExecutionThread"; // edu.gatech.main.MainActivity.TAG;
	private Hashtable<String, JobInstance> jobTable;
	private Socket socket = null;
	//private Context context;
	byte[] mybytearray;

	public OffloadExecutionThread(Socket socket,
			Hashtable<String, JobInstance> _jobTable) { // Context _context) {
		this.jobTable = _jobTable;
		this.socket = socket;
		//this.context = _context;

		new Thread(this, "OffloadExecutionThread").start();
	}

	// 1) accept & parse socket content,
	// 2) find corresponding jobInstance,
	// 3) call jobInstance.startExecution
	// Both OffloadingServer and OffloadingExecutionThread have no realization
	// about persistent or not
	public void run() {

		Log.d(TAG, "New FileReciever, Accepted connection : " + socket);
		long start = System.nanoTime();

		InputStream is = null;
		try {
			// TODO:Cong added this timeout. Make sure you catch it properly
			// socket.setSoTimeout(3000);
			is = socket.getInputStream();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// read meta data
		byte[] mdata = Utility.readNBytes(is, MetaData.METADATA_LENGTH);
		MetaData meta = new MetaData(new String(mdata));

		Log.d(TAG, meta.getLogString());

		// receive data
		int fileLen = meta.getContentLength();
		Log.d(TAG, "Receiving ..." + fileLen);
		byte[] mybytearray = Utility.readNBytes(is, fileLen);

		long end = System.nanoTime();

		Log.d(TAG, "job " + meta.getID() + " Transfer took: " + (end - start));

		try {
			OutputStream os = socket.getOutputStream();
			os.write(1);
			os.flush();

			is.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Executing application
		String jobTableKey = meta.getID();
		JobInstance job = jobTable.get(jobTableKey);

		if (job == null) {
			Log.d(TAG, "Add the job " + jobTableKey + " into the working table");
			// String remoteIP = socket.getRemoteSocketAddress().toString();
			// remoteIP = remoteIP.substring(1, remoteIP.indexOf(':'));

			job = new JobInstance(meta.getID(), Utility.socketToIP(socket) );
					//context);
			jobTable.put(job.getJobTableKey(), job);
		}

		Log.d(TAG, "jobTableKey=" + jobTableKey + ", job=" + job
				+ ", jobTable=" + jobTable);

		if (mybytearray == null)
			Log.d(TAG, "Null mybytearray");
		else
			job.startExecution(meta.getOffloadingMode(), mybytearray);

	}

}
