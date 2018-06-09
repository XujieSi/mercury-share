
public class Main {
	public static void main(String[] args){
		new Thread(new FileSearchServer(), "FileSearchServer").start();
	}
}
