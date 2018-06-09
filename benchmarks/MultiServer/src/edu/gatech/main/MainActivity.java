package edu.gatech.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Hashtable;
import java.util.Scanner;

import edu.gatech.R;
import edu.gatech.availability.StatusCheckServer;
import edu.gatech.jobinstance.JobInstance;
import edu.gatech.offloading.OffloadExecutionServer;


import edu.gatech.test.LatencyTestListener;
import edu.gatech.uselessjunk.ResultServer;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener {
	public static final String TAG = "CirrusServer";
	public static final String TAG1 = "CS";
	public static Hashtable<String, JobInstance> jobTable = new Hashtable<String, JobInstance>();
	public static int numInstances = 0;
	public static Hashtable<Integer, Integer> waitTimes = new Hashtable<Integer, Integer>();
	// public static String phoneIP = "192.168.1.6";
	// public static String phoneIP = "128.61.19.83";
	private OffloadExecutionServer offloadingServer;
	private ResultServer resultServer;
	private StatusCheckServer controlServer;
	private LatencyTestListener latencyServer;

	private Button startButton;
	private Button saveButton;
	public static TextView tv;
	private File root = Environment.getExternalStorageDirectory();
	private Boolean started = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		saveButton = (Button) findViewById(R.id.savebutton);
		saveButton.setOnClickListener(this);

		// Startup all the servers
		//controlServer = new StatusCheckServer(this);
		
		offloadingServer = new OffloadExecutionServer(jobTable, this);
		// resultServer = new ResultServer(this);
		latencyServer = new LatencyTestListener();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public void onClick(View v) {
		if (saveButton.equals(v)) {
			String timestamp = Long.toString(System.currentTimeMillis());
			/*
			 * File fromWifi = new File (root + File.separator + "cirrus" +
			 * File.separator + "WifiValues.txt");
			 * 
			 * File toWifi = new File (root + File.separator + "cirrus" +
			 * File.separator + "WifiValues" + timestamp + ".txt");
			 * fromWifi.renameTo(toWifi);
			 */

			File fromExec = new File(root + File.separator + "cirrus"
					+ File.separator + "RunTimes.txt");

			File toExec = new File(root + File.separator + "cirrus"
					+ File.separator + "RunTimes" + timestamp + ".txt");
			fromExec.renameTo(toExec);

			/*
			 * File fromSend = new File (root + File.separator + "cirrus" +
			 * File.separator + "SendTimes.txt");
			 * 
			 * File toSend = new File (root + File.separator + "cirrus" +
			 * File.separator + "SendTimes" + timestamp + ".txt");
			 * fromSend.renameTo(toSend);
			 * 
			 * File fromHandoff = new File (root + File.separator + "cirrus" +
			 * File.separator + "handoff.txt");
			 * 
			 * File toHandoff = new File (root + File.separator + "cirrus" +
			 * File.separator + "handoff" + timestamp + ".txt");
			 * fromHandoff.renameTo(toHandoff);
			 */
		}

	}
}