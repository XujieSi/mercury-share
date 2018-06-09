package edu.gatech.offloading;

import java.lang.reflect.Method;

//import android.util.Log;
import edu.gatech.protocol.Log;


import edu.gatech.protocol.OffloadingMode;

// I) This is essentially a wrapper of ClientExecutionController
// the only purpose is to make the use of ClientExecutionController more easily.
// II) Assume this is called at server side (server asks client to do something).
//public class RemoteProxyWrapper<T extends Offloadable> {
public class RemoteProxyWrapper<T> {

	private static String TAG = "RemoteProxyWrapper";
	private Boolean callStatically = false;
	
	public RemoteProxyWrapper(){
	}
	
	public RemoteProxyWrapper(Boolean statically){
		callStatically = statically;
	}
	
	
	// assume methodName is unique (no overloading)
	public Object callRemote(T obj, String methodName, Object... args){
		int match = 0;
		Object res = null;
		for(Method m : obj.getClass().getMethods()){
			if(methodName.equals(m.getName())){
				++match;
				if(match > 1){
					Log.d(TAG, "Assumption violated: More than method defined for " + methodName);
					return null;
				}
				
				try{
					ClientExecutionController clientExecutionCtrl = new ClientExecutionController(
								OffloadExecutionServer.whereAmIFrom(), OffloadingMode.PersistentBidirectional);
					
					if(callStatically){
						clientExecutionCtrl.disableInstanceTransfer();
					}
					
					res = clientExecutionCtrl.execute(methodName, m.getParameterTypes(), args, obj);
					
					//res = m.invoke(obj, args);
				}
				catch(Exception e){
					Log.d(TAG, "Remote call for " + methodName + " went wrong ...");
					e.printStackTrace();
				}
			}
		}
		
		if(match == 0){
			Log.d(TAG, "Cannot find a match for method : " + methodName+ ", please make sure it is public");
		}
		
		return res;
	}
}
