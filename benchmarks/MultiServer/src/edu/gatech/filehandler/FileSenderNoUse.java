package edu.gatech.filehandler;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import edu.gatech.protocol.Packet;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
//import android.os.NetworkOnMainThreadException;
import android.util.Log;

public class FileSenderNoUse implements Runnable {
	private Socket socket = null;
	private OutputStream os = null;
	
	private byte[] mybytearray = null;
	private String serverIp = null;
	private static final String TAG = "FileSender";

	public static int SEND_SUCCESS = 0;
	public static int SEND_FAILED = -1;

	private Boolean result = false;
	private String jobKey;

	public FileSenderNoUse(String serverIp, byte[] bufferToSend, String jobKey) {

		this.jobKey = jobKey;
		this.serverIp = serverIp;

		String jKey = jobKey;
		String ex = "X";
		// String bLen = Integer.toString(bufferToSend.length);
		String bLen = String.format("%010d", (bufferToSend.length));
		Log.d(TAG, "Length formatted as " + bLen);
		byte[] metadata = (jKey + ex + bLen).getBytes();

		Log.d(TAG, "metadata is: " + new String(metadata) + "and its length is"
				+ metadata.length);

		mybytearray = new byte[bufferToSend.length + metadata.length];
		System.arraycopy(metadata, 0, mybytearray, 0, metadata.length);
		System.arraycopy(bufferToSend, 0, mybytearray, metadata.length,
				bufferToSend.length);

		Log.d(TAG, "Sendinglength: " + mybytearray.length);

		new Thread(this, "fileSender").start();
	}

	public void run() {
		Log.d(TAG, "in file send");
		try {

			// socket = new Socket();
			// socket.connect(new InetSocketAddress(serverIp, 6789), 10*1000);
			socket = new Socket(serverIp, 6789);

			Log.d(TAG, "!!!...");
			os = socket.getOutputStream();
			Log.d(TAG, "Sending...");
			os.write(mybytearray);
			os.flush();
			Log.d(TAG, "@@@...");
			result = true;
		} catch (IOException e) {
			Log.d(TAG, "Error in socket connect");
			e.printStackTrace();
			result = false;
		}
		/*
		 * catch (NetworkOnMainThreadException n) { Log.d (TAG,
		 * "NOMT Error in socket connect"); result = false; }
		 */finally {
			try {
				if (os != null)
					os.close();
				if (socket != null)
					socket.close();
				if (result.equals(false)) {
					Log.d(TAG, "Returning fail from FIleSender");
					// TODO: set states
					edu.gatech.main.MainActivity.jobTable.get(jobKey).setState(
							edu.gatech.jobinstance.JobInstance.CAN_SEND_RESULT);
					// edu.gatech.main.MainActivity.setServerState(edu.gatech.main.MainActivity.CAN_SEND_RESULT);
				} else {
					Log.d(TAG, "Returning success from FIleSender");
					// TODO: def change this to generic
					edu.gatech.main.MainActivity.jobTable
							.get(jobKey)
							.setState(
									edu.gatech.jobinstance.JobInstance.DONE_SENDING_RESULT);
					// edu.gatech.main.MainActivity.setServerState(edu.gatech.main.MainActivity.IDLE);
				}
				Log.d(TAG, "Set server state");
			} catch (IOException e) {
				Log.d(TAG, "Close problem");
				if (result.equals(false)) {
					Log.d(TAG, "Returning fail from FIleSender");
					edu.gatech.main.MainActivity.jobTable.get(jobKey).setState(
							edu.gatech.jobinstance.JobInstance.CAN_SEND_RESULT);
					// edu.gatech.main.MainActivity.setServerState(edu.gatech.main.MainActivity.CAN_SEND_RESULT);
				} else {
					Log.d(TAG, "Returning success from FIleSender");
					// edu.gatech.main.MainActivity.setServerState(edu.gatech.main.MainActivity.IDLE);
					edu.gatech.main.MainActivity.jobTable
							.get(jobKey)
							.setState(
									edu.gatech.jobinstance.JobInstance.DONE_SENDING_RESULT);
				}
				Log.d(TAG, "Set server state");
			}
		}
	}
}

/*
 * Log.d (TAG, "Creating broadcast"); Intent intent = new Intent();
 * intent.setAction("edu.gatech.ic.prober.CONNECTED_TO_SERVER");
 * intent.putExtra("IPAddress", serverIP); context.sendBroadcast(intent); Log.d
 * (TAG, "Sent broadcast");
 */