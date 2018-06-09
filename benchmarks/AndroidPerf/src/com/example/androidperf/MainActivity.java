package com.example.androidperf;

import java.math.BigInteger;
import java.util.Hashtable;

import edu.gatech.application.ExpensiveWorkload;
import edu.gatech.application.FaceDetection;
import edu.gatech.jobinstance.JobInstance;
import edu.gatech.offloading.ClientExecutionController;
import edu.gatech.offloading.ClientOffloadingTask;
import edu.gatech.offloading.OffloadExecutionServer;
import edu.gatech.offloading.ResultListener;
import edu.gatech.protocol.OffloadingMode;
import edu.gatech.protocol.Utility;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends Activity {

	public static final String serverIP = "128.61.241.240";
	public static Hashtable<String, ClientOffloadingTask> jobTable = new Hashtable<String, ClientOffloadingTask>();
	public static Hashtable<String, JobInstance> serviceTable = new Hashtable<String, JobInstance>();

	public static EditText et1, et2;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		
		//new OffloadExecutionServer(serviceTable, this, serverIP);
		//new ResultListener(jobTable);

		
		et1 = (EditText)this.findViewById(R.id.editText1);		
		et2 = (EditText)this.findViewById(R.id.EditText2);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	
	public void runRemotely(View v){

		int n = Integer.parseInt(et1.getText().toString());
		System.out.println(n);
		long start = SystemClock.currentThreadTimeMillis();
		
		//this.expensive1(n);
		
		OffloadingMode offMode = OffloadingMode.PersistentBidirectional;
		//ClientExecutionController ec = new ClientExecutionController(serverIP, offMode);
		ExpensiveWorkload ew = new ExpensiveWorkload(null,this, n);
		ew.start();

		
		long end = SystemClock.currentThreadTimeMillis();
		et2.setText("Time: "+(end-start));	
	}

	public void startTiming(View v){
		EditText et1 = (EditText)this.findViewById(R.id.editText1);
		EditText et2 = (EditText)this.findViewById(R.id.EditText2);
		int n = Integer.parseInt(et1.getText().toString());
		System.out.println(n);
		long start = SystemClock.currentThreadTimeMillis();
		this.expensive1(n);
		long end = SystemClock.currentThreadTimeMillis();
		et2.setText("Time: "+(end-start));	
	}
	

	public void computationCost(View v){
		int n = 10;
		Utility.logTime("PerfTest", "faceDetection-begin", System.nanoTime());
		
		while(n < 1000){
			int i = 10;
			while( --i >=0 ) {
				Utility.logTime("PerfTest", "RawDataSender-begin" + "\t" + n, System.nanoTime());
				RgbImage.work(n);
				Utility.logTime("PerfTest", "RawDataSender-end" + "\t" + n, System.nanoTime());
			}
			
			n+=n;
		}
		
		Utility.logTime("PerfTest", "faceDetection-begin", System.nanoTime());
		
	}
	
    String expensive(int n){
    	/*
    	BigInteger [][] C = new BigInteger[n][n];
    	C[0][0] = BigInteger.ONE;
    	for(int i=1;i<n;++i){
    		C[i][0] = BigInteger.ONE;
    		C[i][i] = BigInteger.ONE;
    		for(int j=1;j<i;++j){
    			C[i][j] = C[i-1][j].add( C[i-1][j-1] );
    		}
    	}
    	*/
    	BigInteger ret = BigInteger.ONE;
    	
    	BigInteger [] B = new BigInteger[n];
    	B[0] = BigInteger.ZERO;
    	for(int i=1;i<n;++i){
    		B[i] = B[i-1].add( BigInteger.ONE );
    	}
    	
    	BigInteger []  A = new BigInteger[n];
    	for(int i=0;i<n;++i){
    		BigInteger res = BigInteger.ONE;
    		for(int j=0;j<=i;++j){
    			res = res.multiply(B[j]);
    		}
    		A[i] = res;
    		
    		if(  ret.compareTo(res) < 0) ret = res;
    	}
    	
    	String s = "";
    	for(int i=0;i<n;++i) s += B[i];
    	for(int i=0;i<n;++i) s += A[i];
    	
    	return s;
//    	return ret.toString();
    }
    
    void expensive1(int round){
    	int result = 0;
    	for(int i = 0; i < round; i++){
    		int numOper = 8;
    		int opt = (int)(Math.random() * numOper);
    		switch(opt){
    		case 0:
    			result += 3;
    			break;
    		case 1:
    			result -= 3;
    			break;
    		case 2:
    			result *= 3;
    			break;
    		case 3:
    			result /= 3;
    			break;
    		case 4:
    			double temp = (double) result;
    			temp += 3.0;
    			result = (int)temp;
    			break;
    		case 5:
    			temp = (double) result;
    			temp -= 3.0;
    			result = (int)temp;
    			break;
    		case 6:
    			temp = (double) result;
    			temp *= 3.0;
    			result = (int)temp;
    			break;
    		case 7:
    			temp = (double) result;
    			temp /= 3.0;
    			result = (int)temp;
    			break;
    		}
    	}
    }
    
}



