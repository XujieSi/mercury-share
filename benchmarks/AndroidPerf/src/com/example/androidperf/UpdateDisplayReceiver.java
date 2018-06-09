package com.example.androidperf;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.EditText;

public class UpdateDisplayReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub

		Log.d("UpdateDisplayReceiver", "Process intent: " + intent.getAction());

		if (intent.getAction().equals("edu.gatech.JOB_IS_DONE")) {
			String msg = intent.getStringExtra("result_msg");
			
			//com.example.androidperf.MainActivity.et2
			com.example.androidperf.MainActivity.et2.setText(msg);
		} 
		
	}

}
