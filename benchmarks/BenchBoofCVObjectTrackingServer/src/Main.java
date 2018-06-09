import java.io.FileNotFoundException;

import edu.gatech.mcc.objectTracker.Protocol;
import edu.gatech.protocol.Log;


public class Main {
	public static void main(String[] args) throws FileNotFoundException{
		Log.ifPrint = true;
		Protocol.registerKyro(Main.class.getResourceAsStream("classes.txt"));
		new Thread(new BoofCVServer(), "BoofCVServer").start(); 
		
		//new Thread(new UDPThread(), "UDPThread").start();
		//new UDPThread().run();
	}
}
