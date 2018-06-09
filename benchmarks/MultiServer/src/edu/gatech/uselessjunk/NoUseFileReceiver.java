package edu.gatech.uselessjunk;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.util.Arrays;

import edu.gatech.jobinstance.JobInstance;

import android.os.Environment;
import android.util.Log;


public class NoUseFileReceiver implements Runnable{
	private static final String TAG = edu.gatech.main.MainActivity.TAG;
	private Socket socket = null;
	private String fileName = null;
	private static int port = 2345;
	private ServerSocket servsock =  null;
	public Thread t;
	private File root, fileToSave;
	byte [] mybytearray ;
	public NoUseFileReceiver(Socket socket, String fileName) {
		this.socket = socket;
		this.fileName = fileName;
		root = Environment.getExternalStorageDirectory();
		fileToSave = new File (root, fileName);
		this.t = new Thread (this, "FileReceiver");
		t.start();
	}
	public void run(){
	    int filesize=6022386; // filesize temporary hardcoded
        PrintWriter out = null;
        Log.d (TAG, "New FileReciever, Accepted connection : " + socket);
	    long start = System.currentTimeMillis();
	    int bytesRead;
	    int current = 0;
		// receive file
	    mybytearray  = new byte [filesize];
	    InputStream is = null;
		try {
			is = socket.getInputStream();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(fileToSave);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    BufferedOutputStream bos = new BufferedOutputStream(fos);
	    try {
			bytesRead = is.read(mybytearray,0,mybytearray.length);
			current = bytesRead;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	    Log.d (TAG, "Receiving ..." + mybytearray.length);
	    // thanks to A. Cádiz for the bug fix
	    try{
		    do {
		       bytesRead =
		          is.read(mybytearray, current, (mybytearray.length-current));
		       Log.d (TAG, "..." + current);
		       if(bytesRead >= 0) current += bytesRead;
		    } while(bytesRead > -1);
// Here make sure only from 31st byte is written
		    bos.write(mybytearray, 30 , current-30);
		    bos.flush();
	    } catch (IOException e){
	    	e.printStackTrace();
	    }
	    long end = System.currentTimeMillis();
	    //TODO: for some reason, result was enabled despite this thing failing
	    Log.d (TAG, "Transfer took: " + Long.toString(end-start));
	    Log.d (TAG, "Size of mybytearray after copy is " + mybytearray.length);
	    /*byte[] retyrnByte = null;
	    System.arraycopy(mybytearray, 5, retyrnByte, 0, mybytearray.length-4);
	    Log.d (TAG, "Final length = "+retyrnByte.length);*/
	    try {
			bos.close();
			is.close();
			fos.close();
			socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	    Thread thread=  new Thread(){
	        @Override
	        public void run(){
	            try {
	                synchronized(this){
	                    wait(30000);
	                }
	            }
	            catch(InterruptedException ex){                    
	            }
	            Log.d (TAG, "Enabling result");
	            
	            // read metadata: 30 bytes
	            byte[] mData = new byte[30];
	            System.arraycopy(mybytearray, 0, mData, 0, 30);
	            String metadata = new String(mData);
	            
	            Log.d (TAG, "Received metadata: " + metadata);
	            
	            String cID = metadata.substring(0, 17);
	            Log.d (TAG, "Client ID is " + cID);
	            
	            String jID = metadata.substring(17, 19);
	            Log.d (TAG, "Job ID is " + jID);

	            String ex = metadata.substring(19, 20);
	            Log.d (TAG, "Ex is " + ex);
	            
	            int fileLen = Integer.parseInt(metadata.substring(20, metadata.length()));
	            Log.d (TAG, "File Length is " + fileLen);

	            
	            String jobTableKey = JobInstance.getJobTableKeyFromID(cID, jID); 
	            Log.d (TAG, "Key is " + jobTableKey);
	            
	            edu.gatech.main.MainActivity.jobTable.get(jobTableKey).setState
											(edu.gatech.jobinstance.JobInstance.JOB_COMPLETE);
	            edu.gatech.main.MainActivity.jobTable.get(jobTableKey).setState
	            							(edu.gatech.jobinstance.JobInstance.CAN_SEND_RESULT);
	        }
	    };

	    thread.start();  
	    Log.d (TAG, "Leaving File Receiver");
	}
}
