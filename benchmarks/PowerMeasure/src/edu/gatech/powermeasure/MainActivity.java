package edu.gatech.powermeasure;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import edu.gatech.protocol.Utility;


import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener{

	private Button startButton;
	private Button stopButton;
	private Button statusButton;
	private CheckBox uploadBox;
	private TextView statusMsg;
	private EditText et;
	private MeasureTask mt;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		startButton = (Button) findViewById(R.id.StartButton);
		startButton.setOnClickListener(this);
		
		stopButton = (Button)  findViewById(R.id.StopButton);
		stopButton.setOnClickListener(this);
		
		statusButton =  (Button)  findViewById(R.id.StatusButton);
		statusButton.setOnClickListener(this);
		
		uploadBox = (CheckBox) findViewById(R.id.uploadBox);
		
		statusMsg = (TextView) findViewById(R.id.statusText);
		et = (EditText) findViewById(R.id.editText1);
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
		// TODO Auto-generated method stub
	
		if (statusButton.equals(v)) {
			if (mt == null) {
				statusMsg.setText("Measurement has not started yet.");
			} else {
				statusMsg.setText(mt.getMeasureStatus());
			}
		}

		
		if (stopButton.equals(v)) {
			if(mt == null){
				statusMsg.setText("Measurement has not started yet. So no need to stop.");
			}
			else mt.setStop();
			//new TestAvaliableTask().execute(ip);
		}

		if (startButton.equals(v)) {
			statusMsg.setText("Measure has already started ...");

			startButton.setClickable(false);
			uploadBox.setClickable(false);
			et.setEnabled(false);
			
			boolean upload = uploadBox.isChecked();
			int size = 0;
			try{
				size  = Integer.valueOf( et.getText().toString() );
			}
			catch (NumberFormatException e){
				
			}

			if(size > 0)
				mt = new MeasureTask(upload, size);
			else
				mt = new MeasureTask();
			mt.execute();
		}
	}
	
	
	
	class MeasureTask extends AsyncTask<String, Integer, String> {

		Socket socket;
		int count;
		int packetSize;
		boolean upload;
		boolean stop = false;
		
		public MeasureTask(){
			upload = false;
			packetSize = 1000;
		}
		
		public MeasureTask(boolean up, int sz){
			upload = up;
			packetSize = sz;
		}
		
		public void setStop(){
			stop = true;
		}
		
		public String getMeasureStatus(){
			return "Already " + (upload ? "upload" : "download") + " " 
					+ count + " packets (size=" + packetSize+")";
		}
		
		@Override
		protected String doInBackground(String... params) {
			// TODO Auto-generated method stub
			
			// build socket
			String serverIp = "128.61.241.226"; //"192.168.1.116";
			try{
				socket = new Socket(serverIp, 9220);
				socket.setTcpNoDelay(true);
			}
			catch(Exception e){
				e.printStackTrace();
				return "Cannot connect to server: " + serverIp;
			}
			

			byte[] header = ((upload ? "U" : "D")
					+ String.format("%08d", packetSize) + "#").getBytes();
			byte[] body = new byte[packetSize];
			InputStream in = null;
			OutputStream os = null;

			try {
				in = socket.getInputStream();
				os = socket.getOutputStream();
				os.write(header);
			} catch (Exception e) {
				return "Send initial meta data failed";
			}
			
			while (stop == false) {
				try {
					if (upload) {
						os.write(body);
						Utility.readNBytes(in, packetSize);
					} else {
						//android.util.Log.d("PowerMeasure", "start to read...");
						Utility.readNBytes(in, packetSize);
						os.write(body);
						//android.util.Log.d("PowerMeasure", "read is done");
					}
					++count;
				} catch (Exception e) {
					return "Got exception during upload/download";
				}
			}

			
			return "After upload/download " + count + " packets, stop." ;
		}
		
		
		protected void onPostExecute(String msg) {
			startButton.setClickable(true);
			uploadBox.setClickable(true);
			et.setEnabled(true);
			statusMsg.setText(msg);
		}
	}
}
