package edu.gatech.mcc.objectTracker;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Random;
import java.util.Scanner;

import org.ddogleg.fitting.modelset.ransac.Ransac;
import org.objenesis.strategy.StdInstantiatorStrategy;

import boofcv.alg.transform.pyramid.PyramidDiscreteSampleBlur;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageType;
import edu.gatech.util.Utility;



public class Protocol {
	public static int serverListenPort = 9903;
//	public final static String serverIP = "10.0.2.2";
//	public final static String serverIP = "128.61.241.226"; //pag01
//	public final static String serverIP = "192.168.1.122";
	public final static String serverIP = "192.168.2.1";
	//public final static String serverIP = "192.168.1.114";
	//public final static String serverIP = "128.61.241.236"; // fir06
	
//	public final static String serverIP = "192.168.1.105";

	public static int INIT_STATEFUL = 1;
	public static int PROCESS_STATEFUl = 2;
	public static int PROCESS_STATELESS = 3;
	public static boolean hasInit = false;


	public static boolean ifOffload =  true; //  false; //
	public static boolean ifStateful =  false; // true; //   
	
	public static void registerKyro(InputStream configFile){		
		edu.gatech.mcc.stabilization.Protocol.registerKyro(configFile);
	}

}
