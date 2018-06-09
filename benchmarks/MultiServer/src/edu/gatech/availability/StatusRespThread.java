package edu.gatech.availability;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import edu.gatech.filehandler.FileSenderNoUse;
import edu.gatech.jobinstance.JobInstance;
import edu.gatech.protocol.Packet;

import android.content.Context;
import android.util.Log;

public class StatusRespThread implements Runnable {
	private static final String TAG = edu.gatech.main.MainActivity.TAG;
	public Thread t;
	private Socket socket = null;
	private String inputLine = null;
	// private int outputLine2;
	private PrintWriter out;
	private BufferedReader in;
	// private Protocol protocol;
	private static int numInstances = 0;
	private Context context;

	public StatusRespThread(Socket socket, Context context) {
		Log.d(TAG, "New server thread, number " + numInstances);
		numInstances++;
		// this.protocol = protocol;
		this.socket = socket;
		this.context = context;
		this.t = new Thread(this, "ServerThread");
		t.start();
	}

	public void run() {
		try {
			Log.d(TAG, "New connection attempt");
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));

			inputLine = in.readLine();

			Packet packet = new Packet();
			packet = packet.deserialize(inputLine);

			Log.d(TAG, "Received input: msgId=" + packet.getMsgID()
					+ ", clientId=" + packet.getClientID() + ", jobId="
					+ packet.getJobId() + ", senderIP=" + packet.getSenderIP());

			JobInstance job = new JobInstance(packet.getClientID(),
					packet.getJobId(), packet.getSenderIP(), context);

			edu.gatech.main.MainActivity.jobTable
					.put(job.getJobTableKey(), job);

			Log.d(TAG, "Added job with key " + job.getJobTableKey()
					+ " to table, table size is "
					+ edu.gatech.main.MainActivity.jobTable.size());

			Packet outPacket = new Packet(context, packet.getJobId(),
					Packet.SERVER_REPLY);

			out.println(outPacket.serialize());
			out.close();
			in.close();
			socket.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
