package edu.gatech.pag;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Scanner;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Button button = (Button)findViewById(R.id.button1);
		button.setOnClickListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onClick(View v) {
		File dir = Environment.getExternalStorageDirectory();
		File traceInput = new File(dir, "instrTrace.txt"); 
		try {
			Scanner sc = new Scanner(traceInput);
			int uid = sc.nextInt();
			  // Get running processes
	        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
	        List<RunningAppProcessInfo> runningProcesses = manager.getRunningAppProcesses();
	        RunningAppProcessInfo drap = null;
	        if (runningProcesses != null && runningProcesses.size() > 0) {
	        	for(RunningAppProcessInfo rap : runningProcesses){
	        		if(rap.uid == uid){
	        			drap = rap;
	        			android.os.Process.sendSignal(rap.pid,2);//interrupt signal
	        			break;
	        		}
	        	}
	        }
	        Context context = getApplicationContext();
	        int duration = Toast.LENGTH_SHORT;
	        CharSequence text; 
	        if(drap == null){
	        	text = "The process with uid "+uid+" is not running.";
	        }
	        else{
	        	text = "Successfully send interrupt signal to App: "+drap.processName;
	        }
	        Toast toast = Toast.makeText(context, text, duration);
	        toast.show();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

}
