package edu.gatech.nm;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.reflections.Reflections;

import udt.UDTClient;
import udt.UDTInputStream;
import udt.UDTOutputStream;
import udt.UDTSocket;

import edu.gatech.util.Utility;
import edu.gatech.test.LatencyTestListener;
import edu.gatech.test.NetworkMeasureServer;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener {
	static final String sdcard = Environment.getExternalStorageDirectory()
			.getPath();
	static final String logFile = "netowrkMeasure.log";
	static int lastFailurePosition = 0;
	private Button testAvail;
	private Button startMeasure;
	private Button getStatus;
	private TextView tv;
	private EditText et;
	private MeasureTask mt;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		testAvail = (Button) findViewById(R.id.testAvailable);
		testAvail.setOnClickListener(this);

		startMeasure = (Button) findViewById(R.id.startMeasure);
		startMeasure.setOnClickListener(this);

		getStatus = (Button) findViewById(R.id.getStatus);
		getStatus.setOnClickListener(this);

		tv = (TextView) findViewById(R.id.msg);
		et = (EditText) findViewById(R.id.serverIP);
	
		//new NetworkMeasureServer();
		//new LatencyTestListener();
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

	public void onClick(View v) {

		if (getStatus.equals(v)) {
			if (mt == null) {
				tv.setText("Measurement has not started yet.");
			} else {
				tv.setText(mt.getMeasureStatus());
			}

		}

		String ip = et.getText().toString();
		if (testAvail.equals(v)) {
			new TestAvaliableTask().execute(ip);
			//new DumpTask().execute();
		}

		if (startMeasure.equals(v)) {
			tv.setText("Measure has already started ...");

			testAvail.setClickable(false);
			startMeasure.setClickable(false);
			et.setEnabled(false);

			mt = new MeasureTask();
			mt.execute(ip);
			//mt.doInBackground(ip);
		}
	}

	
	class MeasureTask extends AsyncTask<String, Integer, String> {
	//class MeasureTask {
		final int N = 1;//1000;//100000; //1000000; //100; // 10000000;
		//Socket socket;
		DatagramSocket socket;
		InetAddress serverIPAddr;
		
		//UDTSocket socket;
		//UDTClient client;
		
		int cur;
		ArrayList<Integer> measurePoint = new ArrayList<Integer>();
		
		String ip = null;

		String getMeasureStatus() {
			if (cur == measurePoint.size()) {
				return "Measurement is already done!";
			}

			return "Measure data size=" + measurePoint.get(cur) + ", and "
					+ (measurePoint.size() - cur) + " measure points are left";
		}

		void doSomething(){
			int m = 1 << 30;
			int a = 0, b = 1;
			for(int i=0; i < m; ++i){
				a ^= b;
				b ^= a;
				a ^= b;
			}
		}
		
		
		void doSomethingExpensive(){
			int M = 150; // 15 -> 6ms,  17 -> 9ms,   500 -> 3s, 200 -> 1s, 150 -> 200ms
			BigInteger[] res = new BigInteger[M];
			for(int i=0; i < M; ++i){
				res[i] = BigInteger.valueOf(i);
			}
			for(int i=0; i < M; ++i){
				for(int j=0; j < M; ++j){
					res[i] = res[i].add(res[j]);
				}
			}
		}
		
		int measureCurrentPoint(PrintWriter cout, final int sz) {
		
			ArrayList<Long> ls = new ArrayList<Long>();
			long start =0;//= System.nanoTime();
			long all = 0;
			
			
			//int sleep = 300;
			//int sleep = (sz-1) * 25;
			//sz = 5;
			//long sl=0;
			
			try {
				
				//socket.setTcpNoDelay(true);
				//OutputStream os = socket.getOutputStream();
				//InputStream in = socket.getInputStream();

				DatagramSocket os = socket;
				DatagramSocket in = socket;
				
				//UDTOutputStream os = client.getOutputStream();
				//UDTInputStream in = client.getInputStream();
				
				
				byte [] header = (String.format("%09d", sz) + "#").getBytes();
				byte [] body = new byte[sz];
				
				for(int i=0;i<sz;++i) body[i] =  (byte) ((i * i) % 255);
				
				// tell server the client address
				Utility.writeByteUDP(os, serverIPAddr, Utility.UDP_SERVER_PORT, 7, null);

				//os.write(header);
				Utility.simpleProtocolWriteUDP(os, serverIPAddr, Utility.UDP_SERVER_PORT, header, null);

				//android.util.Log.d("NetworkMeasure", "send header");
				
				int cs = 0;
				long t1 = 0, t2 = 0;
				
				start = System.nanoTime();
				while (++cs <= NetworkMeasureServer.iterations) {
					t1 = System.nanoTime();
					
					if (t2 != 0) {
						ls.add(t1 - t2); // positive means D
					}


					// upload
					//os.write(body);
					//os.flush();
					Utility.simpleProtocolWriteUDP(os, serverIPAddr, Utility.UDP_SERVER_PORT, body, null);

					
					t2 = System.nanoTime();
					ls.add(t1 - t2); // negative means U
					
					// download
					//byte[] body2 = Utility.readNBytes(in, sz);
					byte[] body2 = Utility.readNBytesUDP(in, sz);
					

					//verify correctness
					if(body2.length != body.length){
						android.util.Log.d("NetworkMeasure", "Sent and Reveived packet size mismatch");
						return -1;
					}
					//for(int i=0;i<sz ; ++i){
					//	if(body[i] != body2[i]){
					//		android.util.Log.d("NetworkMeasure", "Sent and Reveived packet content mismatch at the " + i + " th byte");
					//		return -1;
					//	}
					//}

					
					long t3 = System.nanoTime();
					all += t3  - t1;
					
					//Thread.sleep(100);
					//Thread.sleep(300);
					Thread.sleep(5000);
					
					/*
					try{
						long sBegin = System.nanoTime();
						if(sleep > 0)
							Thread.sleep(sleep);
						long sEnd = System.nanoTime();
						sl += sEnd - sBegin;						
						//doSomethingExpensive();
						//Thread.sleep(0); //4565579.1196
						//Thread.sleep(20);  // 4753902.034
						//Thread.sleep(50); //  5801727.4516
						//Thread.sleep(100); //   10.14 ms
						//Thread.sleep(200); // 9644492.747
						//Thread.sleep(500); 
					}catch(Exception e){
						android.util.Log.d("NetworkMeasure", "Got exception during sleep");
						e.printStackTrace();
					}*/
					
				}
				
				if (t2 != 0) {
					ls.add(System.nanoTime() - t2);
				}
				
			} catch (Exception e) {
				e.printStackTrace();
				return -1;
			}

			long end = System.nanoTime();
			
			
			//System.out.println( "res : " + (end - start) /  NetworkMeasureServer.iterations  );
			
			
			// output only after all time slots are collected
			for (Long x : ls) {
				if (x > 0)
					cout.println("D\t" + sz + "\t" + x);
				else
					cout.println("U\t" + sz + "\t" + (-x));
			}
			//cout.println("A\t" + NetworkMeasureServer.iterations + "\t" + sz + "\t" + (end - start));
			cout.println("A\t" + NetworkMeasureServer.iterations + "\t" + sz + "\t" + all);
			//cout.println("A\t" + NetworkMeasureServer.iterations + "\t" + sleep + "\t" + all);
			//cout.println("A\t" + NetworkMeasureServer.iterations + "\t" + sleep + "\t" + all + "\t" + sl + "\t" + (end-start));
			
			//cout.println("A\t" + NetworkMeasureServer.iterations + "\t" + sleep + "\t" + (end - start) /  NetworkMeasureServer.iterations );

			return 0;
		}

		void renameLogFile(String newName) {
			File sdcard_dir = Environment.getExternalStorageDirectory();
			File from = new File(sdcard_dir, logFile);
			File to = new File(sdcard_dir, newName);
			from.renameTo(to);
		}

		@Override
		protected String doInBackground(String... params) {

			ip = params[0];
			measurePoint.clear();
			
			// measure 1 bytes ~ 100 ~ 1000 ~ ... ~ 10000000
			// collect data points
			for (int i = 1; i <= N; ++i) {

				//if(i < 90) continue;
				
				int j = i;
				while (j > 100 && j % 10 == 0)
					j /= 10;
				
				if (j <= 100) {
					measurePoint.add(i * 100);
				}
			}

			//measurePoint.add(41);
			// build socket
			try{
				//socket = new Socket(ip, 9220);
				//socket.setTcpNoDelay(true);
				
				serverIPAddr = InetAddress.getByName(ip);
				socket = new DatagramSocket( Utility.UDP_CLIENT_PORT );

				
				//client= new UDTClient(InetAddress.getByName("localhost")); //new UDTClient(InetAddress.getByName("localhost"),12345);
				//client.connect(ip, 9220);
			}
			catch(Exception e){
				e.printStackTrace();
				return "Cannot connect to server: " + ip;
			}
			
			// start measure one by one
			cur = 0;
			PrintWriter pr = null;
			try {
				FileOutputStream fout = new FileOutputStream(sdcard + "/"
						+ logFile);
				pr = new PrintWriter(new BufferedOutputStream(fout));
			} catch (IOException e) {
				e.printStackTrace();
				return "Cannot open log file: " + logFile;
			}

			for (Integer x : measurePoint) {
				if (measureCurrentPoint(pr, x) < 0) {
					pr.close();

					lastFailurePosition = x;
					renameLogFile("nm" + System.currentTimeMillis()
							+ "_failAt_" + x + ".log");
					return "Measure for data size = " + x + " failed!";
				}
				++cur;
			}

			pr.close();
			// rename the log file 
			renameLogFile("nm" + System.currentTimeMillis() + ".log");

			return "All measurement are done. Total points: "
					+ measurePoint.size();
		}

		protected void onPostExecute(String msg) {
			testAvail.setClickable(true);
			startMeasure.setClickable(true);
			et.setEnabled(true);
			tv.setText(msg);
		}
	}

	class TestAvaliableTask extends AsyncTask<String, Integer, Integer> {
		String ip;

		@Override
		protected Integer doInBackground(String... params) {
			try {
				ip = params[0];
				Socket socket = new Socket(ip, 9110);
				OutputStream os = socket.getOutputStream();
				PrintWriter out = new PrintWriter(os, true);
				BufferedReader in = new BufferedReader(new InputStreamReader(
						socket.getInputStream()));

				String stringToSend = "12345567899";
				out.println(stringToSend);
				int a = in.read();
				
				socket.close();
				return 0;

			} catch (Exception e) {
				e.printStackTrace();
			}

			return -1;
		}

		protected void onPostExecute(Integer result) {
			if (result >= 0)
				tv.setText("Good! Server is now avaialbe.");
			else
				tv.setText("Server " + ip + " cannot be reached!!");
		}
	}
}
