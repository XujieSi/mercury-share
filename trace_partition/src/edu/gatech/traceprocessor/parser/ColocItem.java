package edu.gatech.traceprocessor.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.gatech.traceprocessor.utils.ArraySet;
import edu.gatech.traceprocessor.utils.Pair;
import edu.gatech.traceprocessor.utils.Utils;

/**
 * The co-location dictionary item. Co-location works in this way:
 * The methods whose names are in methodList should be potentially co-located.
 * One observation about the native code in Android SDK is that they usually share
 * the native state by passing the address of the C/C++ structure as arguments or return values
 * of the method. Also, native methods of the same java object usually should be co-located together.
 * To incorporate these two observation, we provide argMap to mark the  
 * value to match. ColocConfig specify the way to generate colocation key for a given method.
 * Methods sharing the same colocation key will be co-located together.
 * 
 * @author xin
 *
 */
public class ColocItem {
	/**
	 * The set of names of methods in current co-location set.
	 */
	Set<String> methodList;
	/**
	 * The map from method name to its 
	 */
	Map<String,List<ColocConfig>> argMap;

	static int ID = 0;
	
	int curId;
	
	ColocItem(){
		methodList = new ArraySet<String>();
		argMap = new HashMap<String,List<ColocConfig>>();
		curId = ID++;
	}
	
	void addMethod(String methName, int pos, int length, boolean coSub){
		methodList.add(methName);
		List<ColocConfig> ccs = argMap.get(methName);
		if(ccs == null){
			ccs = new ArrayList<ColocConfig>();
			argMap.put(methName, ccs);
		}
		ccs.add(new ColocConfig(false, pos, length, coSub));
	}
	
	void addMethod(String methName,boolean coSub){
		methodList.add(methName);
		List<ColocConfig> ccs = argMap.get(methName);
		if(ccs == null){
			ccs = new ArrayList<ColocConfig>();
			argMap.put(methName, ccs);
		}
		ccs.add(new ColocConfig(true, -1, -1, coSub));
	}
	
	void interp(String line){
		String segs[] = line.trim().split("\\s+");
		boolean coSub = (Integer.parseInt(segs[1]) > 0)?true:false;
		if(segs.length < 3){
			this.addMethod(segs[0],coSub);
		}else{
			String methName = segs[0];
			int pos = Integer.parseInt(segs[2]);
			int size = Integer.parseInt(segs[3]);
			this.addMethod(methName, pos, size,coSub);
		}
	}
	
	public Set<ColocKey> match(Method m){
		String mName = m.methName();
		Set<ColocKey> ret = new ArraySet<ColocKey>();
		if(methodList.contains(mName)){
			List<ColocConfig> ccs = argMap.get(mName);
			for(ColocConfig cc : ccs){
				try {
					ret.add(new ColocKey(curId,cc,m));
				} catch (ParamOrRetMissingException e) {
					continue;
				}
			}
		}
		return ret;
	}
}

/**
 * This class specify the way we generate co-location key.
 * byNameOnly=true means the token will only be generated globally for the methods with the same name.
 * byNameOnly=false means the token is related with the argument or the return value of the method:
 * start>=0: argument
 * ret<0: return value (-1,-2: lower bits to higher bits of the return value (64bits in dalvik))
 * when colocSub = true, this token is not applied to the method itself, but all its native descents.
 * 
 * @author xin
 *
 */
class ColocConfig{
	boolean byNameOnly;
	int start;
	int size;
	boolean colocSub;
	
	public ColocConfig(boolean byNameOnly, int start, int size, boolean colocSub) {
		super();
		this.byNameOnly = byNameOnly;
		this.start = start;
		this.size = size;
		this.colocSub = colocSub;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (byNameOnly ? 1231 : 1237);
		result = prime * result + (colocSub ? 1231 : 1237);
		result = prime * result + size;
		result = prime * result + start;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ColocConfig other = (ColocConfig) obj;
		if (byNameOnly != other.byNameOnly)
			return false;
		if (colocSub != other.colocSub)
			return false;
		if (size != other.size)
			return false;
		if (start != other.start)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ColocConfig [byNameOnly=" + byNameOnly + ", start=" + start + ", size=" + size + ", colocSub=" + colocSub + "]";
	}
	
}

class ColocKey{
	int id;
	List<Pair<Long,Integer>> value;//the first is the value, the second is to handle garbage collection for addresses
	boolean ifColocSub;// co-locate all the native methods in the subtree
	
	public ColocKey(int id, ColocConfig cc, Method m) throws ParamOrRetMissingException{
		this.id = id;
		this.ifColocSub = cc.colocSub;
		if(cc.byNameOnly)
			return;
		int pos = cc.start;
		int size = cc.size;
		value = new ArrayList<Pair<Long,Integer>>();
		try{
		if(pos < 0){
			pos = 0-pos;
			pos--;
			value = m.ret.subList(pos, pos+size);
		}else{
			value = m.params.subList(pos, pos+size);
		}
		}catch(Exception e){
			Utils.printLogWithTime("WARNING: missing ret or param value, which might be caused by exception handling" );
			Utils.printLogWithTime("Error processing method: "+m);
			Utils.printLogWithTime("Params: "+m.params);
			Utils.printLogWithTime("Return value: "+m.ret);
			throw new ParamOrRetMissingException();
		}
	}
	
	public ColocKey(int id, List<Pair<Long,Integer>> value, boolean ifColocSub){
		this.id = id;
		this.value = value;
		this.ifColocSub = ifColocSub;
	}
	
	public ColocKey normalize(){
		return new ColocKey(id, value, false);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + (ifColocSub ? 1231 : 1237);
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ColocKey other = (ColocKey) obj;
		if (id != other.id)
			return false;
		if (ifColocSub != other.ifColocSub)
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ColocKey [id=" + id + ", value=" + value + ", ifColocSub=" + ifColocSub + "]";
	}

}