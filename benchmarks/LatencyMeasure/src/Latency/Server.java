package Latency;

public abstract class Server implements Runnable{

	public static void main(String[] args){
		
		new TCPServer().run();
		//new UDPServer().run();
		
	}
}
