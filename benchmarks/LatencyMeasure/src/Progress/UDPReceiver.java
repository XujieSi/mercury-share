package Progress;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

import Latency.Utility;

public class UDPReceiver extends Receiver {


	PrintWriter pw;
	DatagramSocket inSocket;
	byte[] receiveData;

	UDPReceiver(){
		try{
			inSocket = new DatagramSocket( Utility.UDP_SERVER_PORT );
			receiveData = new byte[1<<20];
			System.out.println("UDPReceiver initialized, now learning port:" + Utility.UDP_SERVER_PORT );
		}
		catch(Exception e){
			e.printStackTrace();
			System.err.println("Build UDP socket failed.");
		}
		
		try {
			FileOutputStream logFile = new FileOutputStream("UDP_Receiver_" + System.currentTimeMillis() + ".log");
			pw = new PrintWriter(logFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public void confirmClient(String msg, InetAddress clientAddr){
		byte[] buf = msg.getBytes();
		DatagramPacket confirmPacket = new DatagramPacket(buf,
				buf.length,clientAddr, Utility.UDP_CLIENT_PORT);
		try {
			inSocket.send(confirmPacket);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		boolean started = false;
		long t_begin=0, t_end=0;
		long acc_size = 0;
		long acc_packet = 0;
		long measure_size = 0;
		
		try {
			
			while (true) {
				DatagramPacket receivePacket = new DatagramPacket(receiveData,
						receiveData.length);
				inSocket.receive(receivePacket);

				if (receivePacket.getLength() < 20) {
					String s = new String(receivePacket.getData(),
							receivePacket.getOffset(),
							receivePacket.getLength());
					//if (s.equalsIgnoreCase("begin")) {
					if (s.startsWith("begin")) {
						measure_size = Integer.parseInt(s.substring(5));
						//System.out.println("packet size is: " + measure_size);
						confirmClient("ready", receivePacket.getAddress());
						t_begin = System.nanoTime();
						started = true;
						continue;
						
					} else if (s.equalsIgnoreCase("end")) {
						if (started) {
							t_end = System.nanoTime();
							//System.out.println(measure_size + "\t" + acc_packet + "\t" + acc_size + "\t" + (t_end - t_begin));

							pw.println(measure_size + "\t" + acc_packet + "\t" + acc_size + "\t" + (t_end - t_begin));
							pw.flush();
							
							started = false;
							acc_size = 0;
							acc_packet = 0;
						}
						
						confirmClient("done", receivePacket.getAddress());
						continue;
					}
				}

				if (started) {
					acc_size += receivePacket.getLength();
					acc_packet += 1;
				}
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		pw.close();
		// first stage, waiting for packet length
		// waiting for all coming packets, and key an eye on FIN.
		
	}


	@Override
	void receive(Socket s) {
		// TODO Auto-generated method stub
		
	}

}
