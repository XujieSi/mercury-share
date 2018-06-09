package edu.gatech.util;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;
import java.util.Scanner;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;


//import android.util.Log;
import edu.gatech.protocol.Log;
import edu.gatech.protocol.OffloadingMode;


public class Utility {
	public static final int UDP_CLIENT_PORT = 9999;
	public static final int UDP_SERVER_PORT = 8888;
	
	public static final int HEADER_LEN = 10;
	private static String logFile = "/sdcard/offloadingTimeLog.txt";
	private static String inputFile = "/sdcard/in.txt";
	private static OutputStreamWriter logOut = null;
	private static final long RANDOM_SEED = 10101010;
	private static Random R = new Random(RANDOM_SEED);
	public static Kryo kryo = new Kryo();
	public final static double NANO_TO_MILI = 1000000.0;
	public volatile static boolean loop_fake = false;
	
	public static String IOStats_log = null;
	private static OutputStreamWriter io_logOut = null;
	
	
	public static void logIOStats(boolean isSend, long sz){
		if(IOStats_log == null){
			IOStats_log = "/sdcard/io_stats_" + System.currentTimeMillis() + ".log";
			FileOutputStream out;
			try {
				out = new FileOutputStream(logFile, false);
				io_logOut = new OutputStreamWriter(out);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		try {
			io_logOut.write( isSend ? "send\t" : "receive\t" + sz + "\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public final static int LTE_delay = 40;
	
	public static void delay(){
		try{
			Thread.sleep(LTE_delay);
		}catch (InterruptedException ie){
			System.err.println("Got interrupt exception during delay.");
		}
	}
	
	public static void halfDelay(){
		try{
			Thread.sleep(LTE_delay / 2);
		}catch (InterruptedException ie){
			System.err.println("Got interrupt exception during halfDelay.");
		}
	}
	
	
	public static void writeByte(OutputStream out, int content, PrintWriter logger){
		try {
			out.write(content);
			out.flush();
			if(logger != null){
				logger.println("Write: 1");
//				logger.flush();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		logIOStats(true, 1);
	}

	public static void writeByteUDP(DatagramSocket out,  InetAddress dest, int port, int content, PrintWriter logger){
		try {
			//out.write(content);
			byte[] c = new byte[1];
			c[0] = (byte) content;
			DatagramPacket sendPacket = new DatagramPacket(c, c.length, dest, port);
			out.send(sendPacket);
			
			if(logger != null){
				logger.println("Write: 1");
//				logger.flush();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}	

	
	public static void writeByteWithoutFlush(OutputStream out, int content, PrintWriter logger){
		try {
			out.write(content);
			if(logger != null){
				logger.println("Write: 1");
//				logger.flush();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		logIOStats(true, 1);

	}	
	
	static BusySenderRunner bsr = null;
	public static void startLoopSendFake(OutputStream out, InputStream in, int fakeCode, Class lock){
		if(bsr != null)
			return;
		bsr = new BusySenderRunner(fakeCode, out, in, lock);
		new Thread(bsr).start();
	}
	
	
	public static DatagramPacket readByteUDP( DatagramSocket in,  PrintWriter logger){
		try {
			//int ret = in.read();
			
			byte[] receiveData = new byte[1];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			in.receive(receivePacket);
			
			if( logger != null){
				logger.println("Read: 1");
//				logger.flush();
			}
			//return ret;
			return receivePacket;
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static <T> T simpleProtocolReadUDP( DatagramSocket ins, Class<T> type, PrintWriter logger){
		final String TAG = "simpleProtocolRead";
		long start = System.nanoTime();
		
		byte[] header = null;
		//header = Utility.readNBytes(ins, HEADER_LEN);
		header = Utility.readNBytesUDP(ins, HEADER_LEN);

		if (header[HEADER_LEN - 1] != '#') {
			Log.d(TAG,
					"Header format is wrong, the last character is not '#', maybe package size is too large.");
		}

		int len = 0;
		for (int i = 0; i < HEADER_LEN - 1; ++i)
			len = 10 * len + (header[i] - '0');

		Object obj = null;
		
		try {
			//byte[] results = Utility.readNBytes(ins, len);
			byte[] results = Utility.readNBytesUDP(ins, len);

			Input input = new Input(new Input(new ByteArrayInputStream(results)));
			obj = kryo.readClassAndObject(input);
			input.close();
			
		} catch (Exception e) {
			Log.d(TAG,"Failed to parse incoming object ");
			e.printStackTrace();
		}

		long end = System.nanoTime();
//		long end = System.currentTimeMillis();
		Log.d(TAG, "Read object sz=" + (len + HEADER_LEN) + " takes " + (end-start)/1000000.0);
		if(logger!=null){
			logger.println("Read: "+(len+HEADER_LEN));
//			logger.flush();
		}
		return type.cast(obj);
	}

	
	public static void simpleProtocolWriteUDP(DatagramSocket out, InetAddress dest, int port, Object obj, PrintWriter logger){
		final String TAG = "simpleProtocolWrite";
		long start = System.nanoTime();
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		
		try{
			Output output = new Output(bos);
			kryo.writeClassAndObject(output, obj);
			output.close();
		}
		catch(Exception e){
			Log.d(TAG,"Parse result to byte got exception");
			throw new RuntimeException(e);
		}
		
		byte [] body = bos.toByteArray();
		byte [] header = (String.format("%09d", body.length) + "#").getBytes();
		//byte [] overall = new byte[body.length + header.length];
		
		//System.arraycopy(header, 0, overall, 0, header.length);
		//System.arraycopy(body, 0, overall, header.length, body.length);
		
		
		try{
			//out.write(header);
			//out.write(body);
			//out.write(overall);
			//out.flush();
			
			DatagramPacket sendPacket = new DatagramPacket(header, header.length, dest, port);
			out.send(sendPacket);

			sendPacket = new DatagramPacket(body, body.length, dest, port);
			out.send(sendPacket);
		}catch(Exception e){
			Log.d(TAG,"Failed to write result to output stream ");
			e.printStackTrace();
		}
		long end = System.nanoTime();
		
		Log.d(TAG, "write object sz=" + (header.length + body.length) + " takes " + (end - start) / 1000000.0);
		if(logger != null){
			logger.println("Write: "+(header.length+body.length));
//			logger.flush();
		}
	}

	
	
	public static int readByte(InputStream in, PrintWriter logger){
		try {
			int ret = in.read();
			if( logger != null){
				logger.println("Read: 1");
//				logger.flush();
			}
			return ret;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		
	}
	
	
	public static <T> T simpleProtocolRead(InputStream ins, Class<T> type, PrintWriter logger){
		final String TAG = "simpleProtocolRead";
		long start = System.nanoTime();
		//long start = 0;
	
		// first byte is used to indicate start measure time
		/*
		try{
			int res = ins.read();
			if(res != -1) start = System.currentTimeMillis();
		}catch(IOException e){
			e.printStackTrace();
			Log.d(TAG, "read first byte failed");
		}*/
		
		byte[] header = null;
		header = Utility.readNBytes(ins, HEADER_LEN);

		if (header[HEADER_LEN - 1] != '#') {
			Log.d(TAG,
					"Header format is wrong, the last character is not '#', maybe package size is too large.");
		}

		int len = 0;
		for (int i = 0; i < HEADER_LEN - 1; ++i)
			len = 10 * len + (header[i] - '0');

		
		Object obj = null;
		
		try {
			byte[] results = Utility.readNBytes(ins, len);
//Java serialization implementation
//			ByteArrayInputStream bin = new ByteArrayInputStream(results);
//			ObjectInputStream in = new ObjectInputStream(bin);
//			obj = in.readObject();
//			in.close();
			
			Input input = new Input(new Input(new ByteArrayInputStream(results)));
			obj = kryo.readClassAndObject(input);
			input.close();
			
		} catch (Exception e) {
			Log.d(TAG,"Failed to parse incoming object ");
			e.printStackTrace();
		}

		long end = System.nanoTime();
//		long end = System.currentTimeMillis();
		Log.d(TAG, "Read object sz=" + (len + HEADER_LEN) + " takes " + (end-start)/1000000.0);
		if(logger!=null){
			logger.println("Read: "+(len+HEADER_LEN));
//			logger.flush();
		}

		logIOStats(false, len + HEADER_LEN);


		return type.cast(obj);
	}
	
	public static void simpleProtocolWrite(OutputStream out, Object obj, PrintWriter logger){
		final String TAG = "simpleProtocolWrite";
		long start = System.nanoTime();
		
		// notify reader start to measure time
		//try{ out.write(0); out.flush(); }catch(Exception e){}
		
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		
		try{
			//Java serialization
//			ObjectOutput objOut = new ObjectOutputStream(bos); 
//			objOut.writeObject(obj);
//			objOut.close(); 
			
			Output output = new Output(bos);
			kryo.writeClassAndObject(output, obj);
			output.close();
		}
		catch(Exception e){
			Log.d(TAG,"Parse result to byte got exception");
			throw new RuntimeException(e);
		}
		
		byte [] body = bos.toByteArray();
		byte [] header = (String.format("%09d", body.length) + "#").getBytes();
		byte [] overall = new byte[body.length + header.length];
		
		System.arraycopy(header, 0, overall, 0, header.length);
		System.arraycopy(body, 0, overall, header.length, body.length);
		
		
		try{
			//out.write(header);
			//out.write(body);
			out.write(overall);
			out.flush();
		}catch(Exception e){
			Log.d(TAG,"Failed to write result to output stream ");
			e.printStackTrace();
		}
		long end = System.nanoTime();
		
		Log.d(TAG, "write object sz=" + (header.length + body.length) + " takes " + (end - start) / 1000000.0);
		if(logger != null){
			logger.println("Write: "+(header.length+body.length));
//			logger.flush();
		}
	}
	
	
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
	
	public static byte[] readNBytesUDP(DatagramSocket in, int N) {
		
		byte[] res = new byte[N];
		int current = 0;
		int bytesRead = 0;
		try {
			do {
				//bytesRead = is.read(res, current, N - current);
				//current += bytesRead;
				
				DatagramPacket receivePacket = new DatagramPacket(res, current, res.length - current);
				in.receive(receivePacket);				
				current = receivePacket.getLength();
				
				if(bytesRead == -1){
					Log.d("Utility.readNBytes", "bytesRead=-1, inputstream read failed. current read = " + current);
					return null;
				}
				
			} while (current < N);
		} catch (IOException e) {
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
					return null;
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
	
	public static String generateLogNameByTime(){
		return Calendar.getInstance().getTime().toString().replace(' ', '-').replace(':', '-')+".log";
	}

	public static <T> String join(Collection<T> objs, String delimiter) {
		StringBuilder builder = new StringBuilder();

		for (Iterator<T> it = objs.iterator(); it.hasNext();) {
			T obj = it.next();
			builder.append(obj);
			if (it.hasNext()) {
				builder.append(delimiter);
			}
		}

		return builder.toString();
	}	
	
	public static OffloadConfig loadConfig(String fileName){
			OffloadConfig config = new OffloadConfig();
		try {
			String path = "/sdcard/"+fileName;
			Scanner sc = new Scanner(new File(path));
			config.ifOffload = Boolean.parseBoolean(sc.nextLine());
			config.serverIP = sc.nextLine();
			config.port = Integer.parseInt(sc.nextLine());
			config.ifBidirectional = Boolean.parseBoolean(sc.nextLine());
			config.ifPersistent = Boolean.parseBoolean(sc.nextLine());
			return config;
		} catch (FileNotFoundException e) {
			return config;
		}
	}
	
	public static byte[] objectToBytes(Object o){
		ByteArrayOutputStream bos = new ByteArrayOutputStream();	
		ObjectOutput objOut;
		try {
			objOut = new ObjectOutputStream(bos);
			objOut.writeObject(o);
			objOut.close(); 
		} catch (IOException e) {
			throw new RuntimeException(e);
		} 
		return bos.toByteArray();
	}
	
	public static Object bytesToObject(byte[] results){
		ByteArrayInputStream bin = new ByteArrayInputStream(results);
		ObjectInputStream in;
		try {
			in = new ObjectInputStream(bin);
			Object obj = in.readObject();
			bin.close();
			return obj;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}

class BusySenderRunner implements Runnable{
	int code;
	OutputStream out;
	InputStream in;
	Class lock;
	
	public BusySenderRunner(int code, OutputStream out, InputStream in, Class lock){
		this.code = code;
		this.out = out;
		this.in = in;
		this.lock = lock;
	}
	
	@Override
	public void run() {
		while(true){
			if(Utility.loop_fake){
//				try {
//					Thread.sleep(1);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
				synchronized(lock){
					Utility.writeByte(out, code, null);
					Utility.readByte(in, null);
				}
			}
		}
	}
	
}
