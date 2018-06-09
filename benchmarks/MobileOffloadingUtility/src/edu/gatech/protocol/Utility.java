package edu.gatech.protocol;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;
import java.util.Scanner;

//import android.util.Log;
import edu.gatech.protocol.Log;


public class Utility {

	private static String logFile = "/sdcard/offloadingTimeLog.txt";
	private static String inputFile = "/sdcard/in.txt";
	private static OutputStreamWriter logOut = null;
	private static final long RANDOM_SEED = 10101010;
	private static Random R = new Random(RANDOM_SEED);
	
	public static void logTime(String logID, String logType, long absTime){
		if(logOut == null){
			try{
				FileOutputStream out = new FileOutputStream(logFile, false);
				logOut = new OutputStreamWriter(out);
				//logOut.write("logTime\tlogTime\t"+ System.nanoTime()+"\n");
			} catch(IOException e){
				Log.d("logTime", "cannot open offloadingTimeLog.txt");
				e.printStackTrace();
			}
		}
		
		try{
			logOut.write(logID + "\t" + logType + "\t" + absTime+"\n");
			logOut.flush();
		}catch(IOException e){
			Log.d("logTime", "write log failed");
			e.printStackTrace();
		}
	}
	
	
	
	public static double[] pinGetNDoubles(int N){
		double [] res = new double[N];
		try{
			/*
			Scanner cin = new Scanner( new FileInputStream(inputFile));
			int i = 0;
			while(cin.hasNextDouble() && i < N){
				res[i++] = cin.nextDouble();
			}
			
			cin.close();
			
			int j = i;
			while(i < N){
				res[i] = res[i % j];
				++i;
			}
			*/
			
			FileOutputStream out = new FileOutputStream("/sdcard/a.txt");
			//BufferedOutputStream bout = new BufferedOutputStream(out);
			for(int i=0;i<N;++i){
					res [i] = R.nextDouble();
					out.write(0);
			}
			//bout.close();
			out.close();
		}catch (IOException e){
			e.printStackTrace();
		}
		
		return res;
	}
	
	
	public static int[] pinGetNInts(int N){
		int [] res = new int[N];
		try{
			/*
			Scanner cin = new Scanner( new FileInputStream(inputFile));
			int i = 0;
			while(cin.hasNextInt() && i < N){
				res[i++] = cin.nextInt();
			}
			cin.close();
			
			int j = i;
			while(i < N){
				res[i] = res[i % j];
				++i;
			}
			*/
			FileOutputStream out = new FileOutputStream("/sdcard/in.txt");
			//BufferedOutputStream bout = new BufferedOutputStream(out);
			for(int i=0;i<N;++i){
					res [i] = R.nextInt();		
					out.write(0);
			}
			//bout.close();
			out.close();
		}catch (IOException e){
			e.printStackTrace();
		}
		
		return res;
	}
	
	
	public static byte[] readNBytes(InputStream is, int N) {
		
		byte[] res = new byte[N];
		int current = 0;
		int bytesRead = 0;
		try {
			do {
				bytesRead = is.read(res, current, N - current);
				current += bytesRead;
				
				if(bytesRead == -1){
					Log.d("Utility.readNBytes", "bytesRead=-1, inputstream read failed. current read = " + current);
					return res;
				}
				
			} while (current < N);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return res;
	}

	
	
	public static char offloadingModeToChar(OffloadingMode mod) {
		char res = 'X';

		switch (mod) {
		case TransientUnidirectional:
			res = 'A';
			break;
		case PersistentUnidirectional:
			res = 'B';
			break;
		case TransientBidiretional:
			res = 'C';
			break;
		case PersistentBidirectional:
			res = 'D';
			break;
		case NonOffloading:
			res = 'X';
			break;
		default:
			System.err.println("No Matching mode for " + mod);
		}

		return res;
	}

	public static OffloadingMode charToOffloadingMode(char c) {
		OffloadingMode mode = OffloadingMode.NonOffloading;
		switch (c) {
		case 'A':
			mode = OffloadingMode.TransientUnidirectional;
			break;
		case 'B':
			mode = OffloadingMode.PersistentUnidirectional;
			break;
		case 'C':
			mode = OffloadingMode.TransientBidiretional;
			break;
		case 'D':
			mode = OffloadingMode.PersistentBidirectional;
			break;
		case 'X':
			mode = OffloadingMode.NonOffloading;
			break;
		default:
			System.err.println("MetaData cannot recognize current mode : " + c);
		}

		return mode;
	}
	
	public static String socketToIP(Socket socket){
		String remoteIP = socket.getRemoteSocketAddress().toString();
		remoteIP = remoteIP.substring( remoteIP.indexOf('/')+1, remoteIP.indexOf(':'));
		return remoteIP;
	}
	
	public static Boolean isBidirectional(OffloadingMode mod){
		return mod == OffloadingMode.PersistentBidirectional || mod == OffloadingMode.TransientBidiretional;
	}
	public static Boolean isUnidirectional(OffloadingMode mod){
		return mod == OffloadingMode.PersistentUnidirectional || mod == OffloadingMode.TransientUnidirectional;
	}
	
	public static Boolean isPersistent(OffloadingMode mod){
		return mod == OffloadingMode.PersistentBidirectional || mod == OffloadingMode.PersistentUnidirectional;
	}
	public static Boolean isTransient(OffloadingMode mod){
		return mod == OffloadingMode.TransientBidiretional || mod == OffloadingMode.TransientUnidirectional;
	}
	public static Boolean isLocal(OffloadingMode mod){
		return mod == OffloadingMode.NonOffloading;
	}
	
}
