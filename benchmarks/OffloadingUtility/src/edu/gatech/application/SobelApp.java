package edu.gatech.application;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Random;

import edu.gatech.offloading.ClientExecutionController;
import edu.gatech.offloading.RemoteProxyWrapper;
import edu.gatech.protocol.Log;
import edu.gatech.protocol.OffloadingMode;
import edu.gatech.util.Utility;

public class SobelApp implements Serializable{
	private static int rounds = 3;
	private static int iterations = 10;
	private static final long serialVersionUID = 1L;
	public transient ClientExecutionController exectl;
	public transient String TAG = "SobelApp";
	OffloadingMode offMode;
	int [][][] image;

	
	public SobelApp(ClientExecutionController ec, OffloadingMode off){
		exectl = ec;
		offMode = off;
		
		int height = 50;
		int width = 100;
		image = new int[height][][];
		for (int i=0; i<height; i++){
			image[i] = new int[width][];
			for(int j=0; j<width; j++){
				image[i][j] = Utility.pinGetNInts(3);
				for(int k=0;k<3;++k) image[i][j][k] = (image[i][j][k]%3+ 3) %3;
			}
		}
	}
	
	
	public String runTask() {
		//long start = System.currentTimeMillis();
		long start = System.nanoTime();
		Log.d("SobelApp", " work start to run at " + start);
		
		Utility.logTime("" + offMode, "SobelApp-begin", start);
		
		if (Utility.isLocal(offMode)) {
			nonOffloadingExecution();
			return "a";
		}
		
		Class<?>[] paramTypes = { int.class };
		Object[] paramValues = { 0 };
		Object[] results = null;
		try {
			
			//if(Utility.isLocal(offMode)){
			//	testParamTrans(0);
			//
			//else
			
			for (int i = 1; i <= rounds; ++i) {
				if (Utility.isLocal(offMode)) {
					nonOffloadingExecution();
				} else if (Utility.isBidirectional(offMode)) {
					exectl.execute("workBi",paramTypes, paramValues, this);
				} else {
					workUni();
				}

				Log.d("SobelApp-work", "The " + i + "-th round is done");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		long end = System.nanoTime();
		String msg = "running duration :" + (end - start);

		Log.d("SobelApp", " work ended at " + end + ", duration=" + (end-start));
		Utility.logTime("SobelApp", "SobelApp-end", end);
		
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
			Log.d("SobelApp-workUni", "exectl execute workLocally failed");
		}

		readSomeMagicNumber(0);
	}
	
	public void workBi(int noUse){
		for(int i=1;i<=iterations;++i) workLocally( 0 );

		// No matter whether stateful or stateless, client call back is exactly the same, 
		// no need to send whole object back
		RemoteProxyWrapper<SobelApp> wrap = new RemoteProxyWrapper<SobelApp>(true); // assume call static method from client
		wrap.callRemote(this, "readSomeMagicNumber", 0);
	}
	
	public void workLocally(int noUse){
		RgbImage.work(image);
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
