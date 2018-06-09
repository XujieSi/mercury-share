import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

import com.musevisions.android.SudokuSolver.SudokuCore;

import edu.gatech.protocol.Log;
import edu.gatech.util.Utility;

public class SudokuThread implements Runnable{
	String TAG = "SudokuThread";
	Socket inSocket = null;
	OutputStream out;


	public SudokuThread(Socket _in) {
		TAG = TAG + ":" + _in.getPort();
		Log.d(TAG, "Start CollisionThread");
		
		inSocket = _in;
	}
	
	public void run() {
		try {	
			inSocket.setTcpNoDelay(true);
			InputStream ins = inSocket.getInputStream();
			out = inSocket.getOutputStream();
				
			PrintWriter logger = new PrintWriter(System.out);
			// while loop
			while(true){
				//String stations = readString(ins);
				//String pre = readString(ins);
				int[] mPuzzle = Utility.simpleProtocolRead(ins, int[].class, logger);
				long start = System.nanoTime();
				int[] mSolution = SudokuCore.solveMethodOptimised(mPuzzle);
				Log.d(TAG, "Proccing time: "+(System.nanoTime()-start)/Utility.NANO_TO_MILI);
				//Utility.delay();
				Utility.simpleProtocolWrite(out, mSolution, logger);
			}
		} catch (Exception e) {
			Log.d(TAG,"Error: SudokuThread failed to run");
			e.printStackTrace();
		}
	}

}