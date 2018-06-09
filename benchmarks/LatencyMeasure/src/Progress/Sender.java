package Progress;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

public abstract class Sender {
	
	final static int testSize = 100000;
	
	final static int M = 50;
	
	final static int[] measureSizes = {
		101, 132, 233, 426, 818, 1025, 2013, 2530, 3718, 4746, 5273, 7952, 13861, 76892, 93907, 153692, 188908, 307262, 522402, 988908
	};
	
	//final static int[] measureSizes = {
	//	101, 132, 233, 426, 818, 1025, 2013,};
	
	static String ip;
	
	abstract void send(int size);
	
	public static void main(String[] args){
		
		if(args.length !=2) {
			System.out.println("Please specify the ip and log output file");
			return;
		}
		
		ip = args[0];
		
		String logFile = "udpsender_" + args[1];
		FileOutputStream out;
		try {

			out = new FileOutputStream(logFile, false);
			PrintWriter pw = new PrintWriter(out);

			for(int msize : measureSizes){
				
				for(int t = 0; t < M; ++ t){
					long begin = System.nanoTime();
					//new TCPSender().send( msize );
					new UDPSender().send( msize );
					
					long end = System.nanoTime();
					
					//pw.println( "TCP\t" + msize + "\t" + (end - begin) );
					pw.println( "UDP\t" + msize + "\t" + (end - begin) );
					pw.flush();
					
					
					// make sure all previous UDP packets are either lost or delivered.
					
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			
			pw.close();
			
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}		
		
		if(ip.length() > 0){ // use a complex always true check to avoid eclipse warnings.
			return;
		}
		
		
		if(args.length != 2){
			System.out.println("Please specify the ip and packet size");
			return;
		}
		ip = args[0];
		int sz = 0;
		
		try{
			sz = Integer.parseInt(args[1]);
		}catch(NumberFormatException e){
			System.err.println("Wrong number format: " + args[1]);
			System.out.println("Use default packet size: " + testSize);
			sz = testSize;
		}
		
		//new TCPSender().send( sz );
		new UDPSender().send( sz );
		
		System.out.println("Sent " + sz + " bytes to server.");
	}
}
