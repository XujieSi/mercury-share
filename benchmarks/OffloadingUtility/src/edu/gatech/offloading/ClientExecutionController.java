package edu.gatech.offloading;

import java.io.*;
import java.lang.reflect.Method;

//import android.util.Log;
import edu.gatech.protocol.Log;


import edu.gatech.offloading.RawDataSender;
import edu.gatech.protocol.MetaData;
import edu.gatech.protocol.OffloadingMode;
import edu.gatech.util.Utility;

public class ClientExecutionController {
	public static String TAG = "ClientExecutionController";
	
	private ClientOffloadingTask offTask;
	//private long start;
	private byte[] results;
	private int kThExecution;

	private String serverIP;
	private Boolean instanceTransfer;

	OffloadingMode offMode;

	public ClientExecutionController(String _serverIP, OffloadingMode _offMode) {

		Log.d(TAG, "New ExecutinController is constructed");

		this.serverIP = _serverIP;
		this.offMode = _offMode;
		this.results = null;
		this.kThExecution = 0;
		this.instanceTransfer = true;

		this.offTask = new ClientOffloadingTask();
		ResultListener.registerClientOffloadingTask(offTask);
	}
	
	public void disableInstanceTransfer(){
		if( Utility.isTransient(offMode) ){
			Log.d(TAG, "Cannot disable instance transfer (offMode is " + offMode + "), otherwise deserailization error may happen remotely");
			return;
		}
		instanceTransfer = false;
	}

	public Object execute(String methodName, Class<?>[] paramTypes,
			Object[] paramValues, Object instance) throws Exception {
		Log.d(TAG, "ExecutionController starting execute");
		//start = System.currentTimeMillis();
		//Log.d(TAG, "Starting time is: " + start);

		// assume jobId is the same as MetaData ID
		Utility.logTime(offTask.getJobId(), TAG+"-execute-begin", System.nanoTime());
		
		Object result;
		offTask.resetClientState(ClientOffloadingTask.BUTTON_PRESSED);

		if (Utility.isLocal(offMode)) {
			Log.d(TAG, "Executing Locally");
			result = executeLocally(methodName, paramTypes, paramValues,
					instance);
		} else {
			Log.d(TAG, "Executing Remotely");

			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutput out = new ObjectOutputStream(bos);

			++kThExecution;
			if (kThExecution == 1 || Utility.isTransient(offMode)) {
				if(instanceTransfer)
					out.writeObject(instance);
			}

			out.writeObject( instance.getClass() );
			out.writeObject(methodName);
			out.writeObject(paramTypes);
			out.writeObject(paramValues);
			out.close();

			// Get the bytes of the serialized object
			byte[] buf = bos.toByteArray();

			result = executeRemotely(buf);
		}
		
		Utility.logTime(offTask.getJobId(), TAG+"-execute-end", System.nanoTime());

		return result;
	}

	private Object executeLocally(String methodName, Class<?>[] paramTypes,
			Object[] paramValues, Object instance) throws Exception {

		Method method = instance.getClass().getDeclaredMethod(methodName,
				paramTypes);
		return method.invoke(instance, paramValues);
	}

	private Object executeRemotely(byte[] b) throws Exception {
		Log.d(TAG, "In remote Execution");

		offTask.resetClientState(ClientOffloadingTask.BUTTON_PRESSED);
		offTask.setData(b);
		offTask.registerListener(this);

		
		//if(instanceTransfer)
		OffloadingMode mod = kThExecution == 1 && instanceTransfer ? OffloadingMode.TransientUnidirectional
				: offMode;

		MetaData meta = new MetaData(offTask.getJobId(), mod, b.length);

		new RawDataSender(serverIP, OffloadExecutionServer.serverListenPort, b,
				meta);

		// waiting server, until get the result
		synchronized (this) {
			this.wait();
		}

		ByteArrayInputStream bin = new ByteArrayInputStream(results);
		ObjectInputStream in = new ObjectInputStream(bin);
		Object ret = in.readObject();

		return ret;
	}

	public void receiveResult(byte[] buffer) {
		results = buffer;
		synchronized (this) {
			this.notify();
		}
	}
	
	public String getExecJobId(){
		return offTask.getJobId();
	}
}
