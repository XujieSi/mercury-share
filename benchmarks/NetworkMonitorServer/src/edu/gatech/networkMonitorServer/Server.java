package edu.gatech.networkMonitorServer;

public class Server {
	
	public static void main(String[] args){
		System.out.println("Starting Server");
		UDPReceiver receiver = new UDPReceiver();
		receiver.start();
	}

}
 