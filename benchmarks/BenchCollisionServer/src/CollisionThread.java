import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import edu.gatech.mcc.collision.GameObject;
import edu.gatech.mcc.collision.Protocol;
import edu.gatech.protocol.Log;
import edu.gatech.util.Utility;

public class CollisionThread implements Runnable{
	String TAG = "CollisionThread";
	Socket inSocket = null;
	OutputStream out;
	List<GameObject> gos; 


	public CollisionThread(Socket _in) {
		TAG = TAG + ":" + _in.getPort();
		Log.d(TAG, "Start CollisionThread");
		
		inSocket = _in;
		gos = new ArrayList<GameObject>();
	}
	
	public void run() {
		try {	
			inSocket.setTcpNoDelay(true);
			InputStream ins = inSocket.getInputStream();
			out = inSocket.getOutputStream();
			
			
			// while loop
			while(true){
				//String stations = readString(ins);
				//String pre = readString(ins);
				
				long start = System.nanoTime();
			
				int header = ins.read();
				Log.d(TAG, header+" to read.");
				
				if(header == Protocol.SET_OBJ){
					List<GameObject> tmp = Utility.simpleProtocolRead(ins, List.class,null);
					gos = tmp;
					Log.d(TAG, "Load all game objects. Start collision detection");
					long mid_start = System.nanoTime();
					List<Boolean> ret = this.doCollisionDetection();
					long mid_end = System.nanoTime();			
					Log.d(TAG, "Collision detection is done.");
					Utility.delay();
					Utility.simpleProtocolWrite(out, ret,null);
					Log.d(TAG, "write is done.");
					long end = System.nanoTime();
					Log.d(TAG, "All:\t" + (end - start));
					Log.d(TAG, "Essential:\t" + (mid_end - mid_start));
					continue;
				}
				if(header == Protocol.ADD_OBJ){
					List<GameObject> tmp = Utility.simpleProtocolRead(ins, List.class,null);
					gos.addAll(tmp);				
					continue;
				}
				if(header == Protocol.UPDATE_POS){
					List<Integer> pos = Utility.simpleProtocolRead(ins, List.class,null);
					if(pos.size() != gos.size()){
						throw new RuntimeException("The position list length does not match the game object list length!");
					}
					for(int i = 0; i < pos.size(); i++){
						int p = pos.get(i);
						GameObject go = gos.get(i);
						go.x = p%1000;
						go.y = p/1000;
					}
					Log.d(TAG, "update pos is done. Start collision detection");
					long mid_start = System.nanoTime();
					List<Boolean> ret = this.doCollisionDetection();
					long mid_end = System.nanoTime();			
					Log.d(TAG, "Collision detection is done.");
					Utility.delay();
					Utility.simpleProtocolWrite(out, ret,null);
					Log.d(TAG, "write is done.");
					long end = System.nanoTime();
					Log.d(TAG, "All:\t" + (end - start));
					Log.d(TAG, "Essential:\t" + (mid_end - mid_start));
					continue;
				}
				throw new RuntimeException("Unknow header: "+header);
			}
		} catch (Exception e) {
			Log.d(TAG,"Error: StationFilterThread failed to run");
			e.printStackTrace();
		}
	}

	private List<Boolean> doCollisionDetection(){
		List<Boolean> ret = new ArrayList(gos.size());
		OUT:for(GameObject go1 : gos){
			go1.isColed = false;
			for(GameObject go2: gos){
				if(go1 != go2 && go1.intersect(go2)){
					go1.isColed = true;
					ret.add(go1.isColed);
					continue OUT;
				}
			}
			ret.add(go1.isColed);
		}
		return ret;
	}

}