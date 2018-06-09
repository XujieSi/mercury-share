package edu.gatech.offloading;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Hashtable;

import edu.gatech.offloading.ClientOffloadingTask;
import edu.gatech.offloading.ResultReceiver;

//import android.util.Log;
import edu.gatech.protocol.Log;


public class ResultListener implements Runnable {
	public static final int resultListenPort = 9900;
	private static final String TAG = "ResultListener";
	private ServerSocket resultSocket = null;
	private Hashtable<String, ClientOffloadingTask> jobTable = null;
	
	private static ResultListener shareListener = null;
	
	public ResultListener(Hashtable<String, ClientOffloadingTask> _jobTable) {
		
		if(shareListener != null){
			Log.d(TAG, "ShareListener is not null, but we assume there is only resulter listener");
			return;
		}
		
		this.jobTable = _jobTable;
		shareListener = this;
		new Thread(this, "ResultListener").start();
	}

	public static void registerClientOffloadingTask(ClientOffloadingTask task){
		if(shareListener == null){
			Log.d(TAG, "register clientOffloadingTask failed, shareListener is null ,no Listener is started yet");
			return;
		}
		shareListener.jobTable.put(task.getJobId(), task);
		Log.d( TAG, "Task " + task.getJobId() + " is registered listener successfully; current table: " + shareListener.jobTable);
	}
	
	
	
	public void run() {
		boolean listening = true;
		try {
			resultSocket = new ServerSocket(resultListenPort);
			Log.d(TAG, "ServerSocket started on port " + resultListenPort);
		} catch (IOException e) {
			Log.d(TAG, "Could not listen on port: " + resultListenPort);
			System.exit(-1);
		}

		while (listening) {
			try {
				new ResultReceiver(resultSocket.accept(), jobTable);
			} catch (IOException e) {
				Log.d(TAG, "IOEx in Server accept");
				e.printStackTrace();
			}
		}

		Log.d(TAG, "Quitting Result Listener");
		try {
			resultSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
