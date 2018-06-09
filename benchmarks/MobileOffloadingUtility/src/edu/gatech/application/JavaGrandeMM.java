
package edu.gatech.application;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Random;

import edu.gatech.application.JavaGrandeS2.sparsematmult.SparseMatmult;
import edu.gatech.offloading.ClientExecutionController;
import edu.gatech.offloading.RemoteProxyWrapper;
import edu.gatech.protocol.Log;
import edu.gatech.protocol.OffloadingMode;
import edu.gatech.protocol.Utility;

public class JavaGrandeMM implements Serializable{
	private static int rounds = 3;
	private static int iterations = 10;
	private static final long serialVersionUID = 1L;
	public transient ClientExecutionController exectl;
	public transient String TAG = "JavaGrandeHeapSort";
	OffloadingMode offMode;
	
	double[] x;
	double[] y;
	double[] val;
	int[] col;
	int[] row;
	
	public JavaGrandeMM(ClientExecutionController ec, OffloadingMode off){
		exectl = ec;
		offMode = off;
		
		int MRow = 1000;
		int NCol = 1000;
		int sz = 100000;//1000;//100000;
		x = Utility.pinGetNDoubles(NCol);
		y = Utility.pinGetNDoubles(MRow);
		val = Utility.pinGetNDoubles(sz);
		col = Utility.pinGetNInts(sz);
		row = Utility.pinGetNInts(sz);
		
		for(int i=0;i<sz;++i){
			col[i] = (col[i]%NCol + NCol) % NCol;
			row[i] = (row[i]%MRow + MRow) % MRow;
		}
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
		RemoteProxyWrapper<JavaGrandeMM> wrap = new RemoteProxyWrapper<JavaGrandeMM>(true); // assume call static method from client
		wrap.callRemote(this, "readSomeMagicNumber", 0);
	}
	
	public void workLocally(int noUse){
		SparseMatmult.test(y, val, row, col, x, 10);
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
