import org.objenesis.strategy.StdInstantiatorStrategy;

import pag.zxwrapper.Protocol;

import com.google.zxing.BinaryBitmap;

import edu.gatech.util.Utility;


public class Main {
	public static void main(String[] args){
		Protocol.registerKyro();
		new Thread(new ZxingServer(), "ZxingServer").start();
	}
}
