package Progress;

import java.net.Socket;

public abstract class Receiver implements Runnable {

	abstract void receive(Socket s);
	
	public static void main(String [] args){
		
		new TCPReceiver().run();
		//new UDPReceiver().run();
		
	}
}
