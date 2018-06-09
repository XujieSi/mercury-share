package Latency;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public abstract class Client {

	final int N = 1000; //1000000; //1000; //100; // 10000000;
	
	static String logPath = "/Users/xujie/Projects/android_workspace/LatencyMeasure/bin/";
	static String logFile = "network_measure.log";
	static String ip;
	
	ArrayList<Integer> measurePoint;

	
	void fillMeasurePoints(){
		measurePoint = new ArrayList<Integer>();
		for (int i = 1; i <= N; ++i) {
			int j = i;
			while (j > 100 && j % 10 == 0)
				j /= 10;
			if (j <= 100) {
				measurePoint.add(i);
			}
		}
	}
	
	abstract void setupCommunication(int packetSize) throws IOException;
	abstract long measureCurrentPoint(PrintWriter pr, int packetSize) throws IOException;
	
	void runMeasurement(){
		fillMeasurePoints();
		PrintWriter pr = null;
		try {
			FileOutputStream fout = new FileOutputStream(logPath + "/"
					+ logFile);
			pr = new PrintWriter(new BufferedOutputStream(fout));
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println( "Cannot open log file: " + logFile );
		}
		
		for (Integer x : measurePoint) {
			try{
				
				setupCommunication(x);

				int i = Utility.MeasureIterations;
				long res = 0; // only useful for UDP measurement
				long start = System.nanoTime();
				while( --i >= 0){
					res  += measureCurrentPoint(pr, x);
				}
				long end = System.nanoTime();
				
				if(res == 0){ // record TCP measurement
					pr.println("A\t" + Utility.MeasureIterations + "\t" + x + "\t" + (end - start));
				}
				else{ // record UDP measurement
					assert res <= (end - start) : "Time measurement is probably wrong!";
					pr.println("A\t" + Utility.MeasureIterations + "\t" + x + "\t" + res);					
				}
				
				pr.flush();
				System.out.println("Finished measuring packet size: " + x);
				
			}catch(Exception e){
				e.printStackTrace();
				pr.close();
				Utility.renameLogFile(logPath, logFile, "nm" + System.currentTimeMillis()
						+ "_failAt_" + x + ".log");
			}
		}
		
		//String prefix = "nm_";
		//String prefix = "Klaus_2nd_floor_pingpong_";
		//String prefix = "Klaus_2nd_floor_stairs_";
		//String prefix = "Klaus_3rd_floor_near3201_";
		//String prefix = "Klaus_3rd_floor_near3201_";
		//String prefix = "BillClass_";
		//String prefix = "BillClassMiddle_";
		//String prefix = "Bench_outside_Klaus_";
		//String prefix = "Bench_outside_COC_";
		//String prefix = "ME_parking_lot";
		//String prefix = "Foodcourt_2nd_floor_";
		//String prefix = "Techgreen_";
		//String prefix = "clough_starbucks_";
		//String prefix = "clough_library_";
		//String prefix = "CS7001_classroom_";
		String prefix = "Galaxy_S3_";
		
		
		
		
				
		Utility.renameLogFile(logPath, logFile, prefix + N + "_" + System.currentTimeMillis());
	}
	
	public static void main(String [] args){
		if(args.length != 1){
			System.out.println("Please specify the ip address");
			return;
		}
		ip = args[0];
		
		logPath = "/sdcard/";
		
		new TCPClient().runMeasurement();
		//new UDPClient().runMeasurement();
	}
}
