import edu.gatech.protocol.Log;


public class Main {
	public static void main(String[] args){
		Log.ifPrint = true;
		new StationFilterServer();
		//new StationFilterThreadUDP().run();
		
	}
}
