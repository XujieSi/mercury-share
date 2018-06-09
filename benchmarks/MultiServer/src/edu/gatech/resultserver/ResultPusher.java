package edu.gatech.resultserver;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import edu.gatech.filehandler.FileSenderNoUse;
import edu.gatech.jobinstance.JobInstance;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class ResultPusher extends BroadcastReceiver{
	
	private static final String TAG = edu.gatech.main.MainActivity.TAG;
	public ResultPusher() {
		Log.d (TAG, "In result pusher");
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Bundle extras = intent.getExtras();
		
		if (intent.getAction().equals("edu.gatech.jobinstance.CAN_SEND_RESULT")) {
			Log.d (TAG, "Sending Result");
			String thisJobId = intent.getStringExtra("jobId");
			String thisClientId = intent.getStringExtra("clientId");
			String jobKey = JobInstance.getJobTableKeyFromID(thisClientId, thisJobId);
			String clientIP = edu.gatech.main.MainActivity.jobTable.get(jobKey).getClientIP();

			Object result = edu.gatech.main.MainActivity.jobTable.get(jobKey).getResult();
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			try{
				ObjectOutput out = new ObjectOutputStream(bos); 
				out.writeObject(result);
				out.close(); 
			}
			catch(Exception e){}
			byte[] buf = bos.toByteArray(); 
			/*
        	 * This part starts a result pusher IF the state allows it to.
        	 * 
        	 */
			
			Log.d (TAG, "Using key: " + jobKey);
    		Log.d (TAG, "Sending Result to clientIP " + clientIP);
    		
    		
    		/*File fileToSend = new File (Environment.getExternalStorageDirectory()
    										+ File.separator +  "result.txt");
    		FileInputStream is;
			byte [] byteArray = null;
			try {
				is = new FileInputStream(fileToSend);
				byteArray = IOUtils.toByteArray(is);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
			
    		FileSenderNoUse resultFileSender = new FileSenderNoUse (clientIP, buf, jobKey);
    		
    		edu.gatech.main.MainActivity.jobTable.get(jobKey).setState
								(edu.gatech.jobinstance.JobInstance.SENDING_RESULT);
    		
    		//edu.gatech.main.MainActivity.setServerState(edu.gatech.main.MainActivity.SENDING_RESULT);
        	/* ------------------result pusher ends------------------*/
		}
	}

}
