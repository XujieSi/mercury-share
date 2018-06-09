package edu.gatech.offloading;


//import android.util.Log;
import edu.gatech.protocol.Log;


public class ClientOffloadingTask {
	private static final String TAG = "OffloadingTask";

	public static int globalJobId = 0;

	public static final int PING_RESULT_SERVER = -2;
	public static final int INIT = 0;
	public static final int BUTTON_PRESSED = -1;
	public static final int START_JOB = 1;
	public static final int SEND_CODE = 2;
	public static final int SENDING_CODE = 3;
	public static final int SENDING_DATA = 4;
	public static final int WAITING_FOR_RESULT= 5;
	public static final int DONE = 100;

	private int taskState;
	private byte[] taskData;
	private ClientExecutionController exectl;
	private int taskId;

	public Boolean sentFile = false;
	public Boolean sendingFile = false;

	public ClientOffloadingTask(int dummy){
		taskId = 99;
	}
	
	public ClientOffloadingTask() {
		Log.d (TAG, "Creating ClientObj");
		
		++globalJobId;
		
		taskId = 10 + (globalJobId%90);// make sure the id always has two digits
//		setClientState(INIT);
		setClientState(BUTTON_PRESSED);
	}
	
	public int getClientState() {
		return taskState;
	}
	public void setClientState(int clientState) {
		
		if(clientState == DONE) Log.d(TAG, "Will set client state to DONE");
		
		if (this.taskState == DONE){
			Log.d(TAG, "Trying to Change from DONE state for JobID " + this.taskId);
		} else {
			this.taskState = clientState;
		}
	}
	
	public void resetClientState(int clientState){
		this.sentFile = false;
		this.sendingFile = false;
		this.taskState = clientState;
	}
	
	public void setData(byte[] data){
		taskData = data;
	}
	public byte[] getData(){
		return taskData; 
	}
	
	public void registerListener(ClientExecutionController ec){
		exectl = ec;
	}
	public void receiveResult(byte[] buffer){
		
		if(exectl == null) Log.d(TAG, "exectl is null, cannot receive the result!");
		else exectl.receiveResult(buffer);
		
		taskState = DONE;
	}
	public void cleanup(){
		taskData = null;
		taskState = INIT;
		exectl = null;
	}
	
	public String getJobId(){
		return Integer.toString(this.taskId);
	}

}
