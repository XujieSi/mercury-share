package edu.gatech.uselessjunk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import edu.gatech.filehandler.FileSenderNoUse;
import edu.gatech.jobinstance.JobInstance;
import edu.gatech.protocol.Packet;

import android.content.Context;
import android.util.Log;


public class ResultServerThread implements Runnable{
	private static final String TAG = edu.gatech.main.MainActivity.TAG;
    public Thread t;
    private Socket socket = null;
    private String inputLine = null;
    private int outputLine;
    private PrintWriter out;
    private BufferedReader in;
    private static int numInstances = 0;
    private Context context;
    public ResultServerThread(Socket socket, Context context) {
    	Log.d (TAG, "New result server thread, number " + numInstances);
    	this.socket = socket;
    	this.context = context;
    	numInstances++;
    	this.t = new Thread(this, "ServerThread");
    	t.start();
    }
    
    public void run() {
    	try{
    		Log.d (TAG, "New connection attempt");
    	    out = new PrintWriter(socket.getOutputStream(), true);
    	    in = new BufferedReader( 
    				    new InputStreamReader(
    				    socket.getInputStream()));
    	    
        try{
        	inputLine = in.readLine();
        	while (inputLine==null) ;
        	Log.d (TAG, "Received input: " + inputLine);
        	Packet inPacket = new Packet();
        	inPacket = inPacket.deserialize(inputLine);
        	String jobTableKey = JobInstance.getJobTableKeyFromPacket(inPacket);
        	
        	
        	outputLine = edu.gatech.protocol.Packet.SERVER_REPLY;
        	Packet outPacket = new Packet(context, inPacket.getJobId(), outputLine);
        	out.println(outPacket.serialize());
        	Log.d (TAG, "Server output: " + outputLine);
        	out.println(outputLine);
        	
        	
        	// this looks odd, but remember that set state has a broadcast sender
        	if (edu.gatech.main.MainActivity.jobTable.get(jobTableKey)!=null && 
        			edu.gatech.main.MainActivity.jobTable.get(jobTableKey).getState() 
        								== edu.gatech.jobinstance.JobInstance.CAN_SEND_RESULT) {
        		edu.gatech.main.MainActivity.jobTable.get(jobTableKey).setState
        					(edu.gatech.jobinstance.JobInstance.CAN_SEND_RESULT);
        	}
        	/*
        	 * This part starts a result pusher IF the state allows it to.
        	 * TODO: make this a file tranfer
        	 * TODO: add intermittent connectivity here 
        	 */

/*        	if (edu.gatech.main.MainActivity.getServerState() == edu.gatech.main.MainActivity.CAN_SEND_RESULT){
        		Log.d (TAG, "Sending Result");
        		FileSender resultFileSender = new FileSender (phoneIp, "result.txt");
        		edu.gatech.main.MainActivity.setServerState(edu.gatech.main.MainActivity.SENDING_RESULT); 
        	}
*/
        	Log.d (TAG, "Back to loop");
	        } catch (IOException e) {
	        	Log.d (TAG, "Read failed");
	        }
    	    Log.d  (TAG, "Quitting Server Instance");
    	    
    	    out.close();
    	    in.close();
    	    socket.close();

    	} catch (IOException e) {
    	    e.printStackTrace();
    	}
     	
    }

}
