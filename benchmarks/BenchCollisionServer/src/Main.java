
public class Main {
	public static void main(String[] args){
		new Thread(new CollisionServer(), "FileSearchServer").start();
	}
}
