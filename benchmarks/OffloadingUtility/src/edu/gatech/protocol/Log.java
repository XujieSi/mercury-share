package edu.gatech.protocol;

public class Log {
	public static boolean ifPrint = true;
	public static void d(String TAG, String msg){
		if(ifPrint)
			System.out.println(TAG + " : " + msg);
	}
}
