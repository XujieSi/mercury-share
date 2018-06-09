
package edu.gatech.application;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Random;

import edu.gatech.application.JavaGrandeS2.crypt.JGFCryptBench;
import edu.gatech.offloading.ClientExecutionController;
import edu.gatech.offloading.RemoteProxyWrapper;
import edu.gatech.protocol.Log;
import edu.gatech.protocol.OffloadingMode;
import edu.gatech.protocol.Utility;

public class JavaGrandeCrypt implements Serializable{
	private static int rounds = 3;
	private static int iterations = 10;
	private static final long serialVersionUID = 1L;
	public transient ClientExecutionController exectl;
	public transient String TAG = "JavaGrandeCrypt";
	OffloadingMode offMode;
	
	byte[] text;
	
	public JavaGrandeCrypt(ClientExecutionController ec, OffloadingMode off){
		exectl = ec;
		offMode = off;
		
		int sz = 1<<16;
		text = new byte[sz];
		int [] x = Utility.pinGetNInts(sz);
		for(int i=0;i<sz;++i) text[i] = (byte)x[i];
	}
	
	
	public String runTask() {
		//long start = System.currentTimeMillis();
		long start = System.nanoTime();
		Log.d(TAG, " work start to run at " + start);
		
		Utility.logTime("" + offMode, TAG + "-begin", start);
		
		Class<?>[] paramTypes = { int.class };
		Object[] paramValues = { 0 };
		try {
			
			for (int i = 1; i <= rounds; ++i) {
				if (Utility.isLocal(offMode)) {
					nonOffloadingExecution();
				} else if (Utility.isBidirectional(offMode)) {
					exectl.execute("workBi",paramTypes, paramValues, this);
				} else {
					workUni();
				}

				Log.d(TAG + "-work", "The " + i + "-th round is done");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		long end = System.nanoTime();
		String msg = "running duration :" + (end - start);

		Log.d(TAG, " work ended at " + end + ", duration=" + (end-start));
		Utility.logTime(TAG, "SobelApp-end", end);
		
		return msg;
	}
	
	public void nonOffloadingExecution(){
		for(int i=1;i<=iterations;++i) workLocally( 0 );
		readSomeMagicNumber(0);
	}
	
	public void workUni(){
		Class<?>[] paramTypes = { int.class };
		try {
			for(int i=1;i<=iterations;++i){
				Object[] paramValues = { 0 };
				exectl.execute("workLocally", paramTypes,
						paramValues, this);
			}
		} catch (Exception e) {
			Log.d(TAG + "-workUni", "exectl execute workLocally failed");
		}

		readSomeMagicNumber(0);
	}
	
	public void workBi(int noUse){
		for(int i=1;i<=iterations;++i) workLocally( 0 );

		// No matter whether stateful or stateless, client call back is exactly the same, 
		// no need to send whole object back
		RemoteProxyWrapper<JavaGrandeCrypt> wrap = new RemoteProxyWrapper<JavaGrandeCrypt>(true); // assume call static method from client
		wrap.callRemote(this, "readSomeMagicNumber", 0);
	}
	
	public void workLocally(int noUse){
		JGFCryptBench b = new JGFCryptBench();
		b.genTestData(text);
		b.JGFkernel();
	}
	
	
	public static Integer readSomeMagicNumber(int x) {
		Random random = new Random();
		Integer res = random.nextInt();
		
		try{
			FileOutputStream out = new FileOutputStream("/sdcard/a.txt");
			out.write(res);
			out.close();
		}
		catch(IOException e){
			e.printStackTrace();
		}
		
		Log.d("readSomeMagicNumber", "the magic number is " + res);
		return res;
	}

}
