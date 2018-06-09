package edu.gatech.jobinstance;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;

//import dalvik.system.DexClassLoader;



import edu.gatech.offloading.ResultListener;
import edu.gatech.offloading.RawDataSender;
import edu.gatech.offloading.OffloadExecutionServer;
import edu.gatech.protocol.CustomObjectInputStream;
import edu.gatech.protocol.MetaData;
import edu.gatech.protocol.OffloadingMode;

//import android.content.Context;
//import android.util.Log;
import edu.gatech.protocol.Log;
import edu.gatech.util.Utility;


// Used on server side
public class JobInstance {
	private static final String TAG = "JobInstance";

	private String jobTableKey;
	private String jobId;
	
	private String clientIP;
	private int state;
	public static int INIT = 0;

	public static int CAN_SEND_RESULT = 1;
	public static int SENDING_RESULT = 2;
	public static int JOB_COMPLETE = 3;
	public static int DONE_SENDING_RESULT = 4;
	//private Context context;
	private Object result;
	private Object instance;

	public JobInstance(String jobId, String clientIP ) {
			//Context context) {
		//if (context == null)
		//	Log.d(TAG, "JI received Null context");

		this.instance = null;
		this.result = null;

		this.setJobId(jobId);
		this.setClientIP(clientIP);
		//this.context = context;
		this.state = INIT;
		this.setJobTableKey(jobId);

	}

	//public static String getJobTableKeyFromPacket(Packet packet) {
	//	return (packet.getClientID() + packet.getJobId());
	//}

	public static String getJobTableKeyFromID(String cId, String jId) {
		return (cId + jId);
	}

	public String getJobId() {
		return jobId;
	}

	private void setJobId(String jobId) {
		this.jobId = jobId;
	}

	public String getClientIP() {
		return clientIP;
	}

	public void setClientIP(String clientIP) {
		this.clientIP = clientIP;
	}

	public void setState(int x){
		// the only purpose is to make some useless code to compile
	}
	
	public int getState() {
		return state;
	}


	public String getJobTableKey() {
		return jobTableKey;
	}

	public void setJobTableKey(String jobTableKey) {
		this.jobTableKey = jobTableKey;
	}

	public void startExecution(OffloadingMode mode, byte[] buf) {
		Log.d(TAG, "Starting execution for " + this.jobId + ", offMod=" + mode);

		if (instance == null && mode != OffloadingMode.TransientUnidirectional) {
			Log.d(TAG,
					"instance is not sent yet, but the offMode is not uni-stateless (assume only uni-stateless carry instance ojb)");
			Log.d(TAG, "Warning: continue executing, assume only static method will be invoked");
			//return;
		}

		//File dexOutputDir = context.getDir("dex", 0);
		//String dexOutputPath = dexOutputDir.getAbsolutePath();

		//DexClassLoader classLoader = new DexClassLoader(
		//		OffloadExecutionServer.appAPK, dexOutputPath, null,
		//		getClass().getClassLoader());
		
		CustomObjectInputStream in = null;
		Method method = null;
		Object[] params = null;
		
		try {
			in = new CustomObjectInputStream(new ByteArrayInputStream(buf),
					null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		if ( Utility.isTransient(mode)) {
			try{
				instance = (Object) in.readObject();
			}catch(Exception e){
				Log.d(TAG, "Read instance object got exception");
			}
		}

		try{
			Class<?> klass = (Class<?>) in.readObject();
			String methodName = (String) in.readObject();
			Class<?>[] paramTypes = (Class<?>[]) in.readObject();
			params = (Object[]) in.readObject();
			in.close();

			if(instance == null){
				method = klass.getDeclaredMethod(methodName, paramTypes); 
			}
			else{
				method = instance.getClass().getDeclaredMethod(methodName,
					paramTypes);
			}

			
			//set offloading status
			//"setOffloadingStatus".invoke(instance, offMod)
			
			
			
			result = method.invoke(instance, params);
			
			Log.d(TAG, "JobInstance has already done current job, will send result");
			sendResult();
		}
		catch (Exception e){
			Log.d(TAG, "JobInstance may fail to execute, got exception");
			e.printStackTrace();
		}
		
	}

	public void sendResult(){
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try{
			ObjectOutput out = new ObjectOutputStream(bos); 
			out.writeObject(result);
			out.close(); 
		}
		catch(Exception e){
			Log.d(TAG, "Parse result to byte got exception");
		}
		
		byte[] buf = bos.toByteArray(); 
		MetaData meta = new MetaData( jobId, OffloadingMode.NonOffloading, buf.length);
		new RawDataSender( clientIP, ResultListener.resultListenPort, buf, meta );
	}
	

	public Object getResult() {
		return result;
	}

	public Object getInstance() {
		return instance;
	}

	public void setInstance(Object obj) {
		instance = obj;
	}
}
