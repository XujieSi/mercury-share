package edu.gatech.application;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import edu.gatech.offloading.ClientExecutionController;
import edu.gatech.offloading.Offloadable;
import edu.gatech.offloading.RemoteProxyWrapper;

public class ExpensiveWorkload extends Offloadable{

	private static final long serialVersionUID = 1L;
	private static String TAG = "ExpensiveWorkload";
	private int round;
	
	public ExpensiveWorkload(ClientExecutionController ec, Context _context, int _round) {
		super(ec, _context);
		
		// TODO Auto-generated constructor stub
		round = _round;
		Log.d(TAG, "ExpensiveWorkload is started to run...");
		
	}

	@Override
	public void runOffloading() {
		// TODO Auto-generated method stub
		RemoteProxyWrapper<ExpensiveWorkload> proxy = new RemoteProxyWrapper<ExpensiveWorkload>();
	
		long begin = System.nanoTime();
		proxy.callRemote(this, "expensive1", round);
		long end =  System.nanoTime();
		
		String msg = "runOffloading takes " + (end - begin) + " ns";
		notifyJobIsDone2(msg);
	}
	
	
	public void notifyJobIsDone2(String msg){
		if(context == null){
			Log.d(TAG, "context is null, cannot notify job is done.");
			return; 
		}
		
		Intent intent = new Intent();
		intent.setAction("edu.gatech.JOB_IS_DONE");
		intent.putExtra("result_msg", msg);
		context.sendBroadcast(intent);
	}
	
    public void expensive1(int round){
    	int result = 0;
    	for(int i = 0; i < round; i++){
    		int numOper = 8;
    		int opt = (int)(Math.random() * numOper);
    		switch(opt){
    		case 0:
    			result += 3;
    			break;
    		case 1:
    			result -= 3;
    			break;
    		case 2:
    			result *= 3;
    			break;
    		case 3:
    			result /= 3;
    			break;
    		case 4:
    			double temp = (double) result;
    			temp += 3.0;
    			result = (int)temp;
    			break;
    		case 5:
    			temp = (double) result;
    			temp -= 3.0;
    			result = (int)temp;
    			break;
    		case 6:
    			temp = (double) result;
    			temp *= 3.0;
    			result = (int)temp;
    			break;
    		case 7:
    			temp = (double) result;
    			temp /= 3.0;
    			result = (int)temp;
    			break;
    		}
    	}
    }

}
