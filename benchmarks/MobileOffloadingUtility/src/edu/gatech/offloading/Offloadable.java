package edu.gatech.offloading;

import java.io.Serializable;

import android.content.Context;
import android.content.Intent;
//import android.util.Log;
import edu.gatech.protocol.Log;


import edu.gatech.offloading.ClientExecutionController;


public abstract class Offloadable extends Thread implements Serializable{
	private static final String TAG = "Offloadable";
	public transient ClientExecutionController exectl;
	public transient Context context;
	
	public Offloadable(ClientExecutionController ec, Context _context){
		exectl = ec;
		context = _context;
	}
	
	@Override
	public void run(){
		runOffloading();
	}
	
	public abstract void runOffloading();
	
	public final void notifyJobIsDone(String msg){
		if(context == null){
			Log.d(TAG, "context is null, cannot notify job is done.");
			return; 
		}
		if(exectl == null){
			Log.d(TAG, "exectl is null, cannot notify which job done.");
			return; 		
		}
		
		Intent intent = new Intent();
		intent.setAction("edu.gatech.JOB_IS_DONE");
		intent.putExtra("jobId", exectl.getExecJobId());
		intent.putExtra("result_msg", msg);
		context.sendBroadcast(intent);
	}
	
}
