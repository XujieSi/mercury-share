package old.edu.gatech.traceprocessor;


import java.util.*;

import edu.gatech.traceprocessor.utils.SortedList;


public class VarEntry {
	private static int GID=0;
	
	private int vid;
	private String value;
	private int size;
	private Function writer;
	private int offset;
//	private Hashtable<Integer, Function> readers;
	private Set<Function> readers;
	public final static List<VarEntry> domain = new ArrayList<VarEntry>();
	
	public VarEntry(String addr, int s, int o, Function wr){
		value = addr;
		size = s;
		offset = o;
		writer = wr;
		readers =  new HashSet<Function>();
		vid = GID;
		GID++;
		domain.add(vid, this);
	}
	
	public VarEntry(VarEntry copy){
		value = copy.value;
		size = copy.size;
		offset = copy.offset;
		readers = new HashSet<Function>();
		vid = GID;
		GID++;
		domain.add(vid, this);
	}
	
	public void setWriter(Function writer){
		this.writer = writer;
	}
	
	public void addReader(Function reader){
	/*	if(readers.get(new Integer(reader.mId)) != null){
			//Utils.printLog("Same variable read twice by a function");
			return;
		}
		readers.put(new Integer(reader.mId), reader);
	*/	
		readers.add(reader);
	}

	public int getVid() {
		return vid;
	}

	public String getValue() {
		return value;
	}

	public int getSize() {
		return size;
	}

	public int getOffset(){
		return offset;
	}
	
	public Function getWriter() {
		return writer;
	}

/*	public Hashtable<Integer, Function> getReaders() {
		return readers;
	}
*/
	
	public Set<Function> getReaders() {
		return readers;
	}
	
	public boolean isThreadShared(){
		Set<Integer> threads = new HashSet<Integer>();
		threads.add(writer.getThreadID());
		for(Function r:readers)
			threads.add(r.getThreadID());
		return threads.size() > 1;
	}
	
	@Override
	public int hashCode(){
		return vid;
	}

	/**
	 * Use with caution!!!!!!
	 * @param size
	 */
	public VarEntry changeSize(int size){
		VarEntry ret = new VarEntry(this.value,size,this.offset,this.writer);
		ret.readers = new HashSet<Function>(this.readers);
		return ret;
	}
	
	@Override
	public boolean equals(Object y){
		if(y == this)
			return true;
		if(y == null)
			return false;
		
		if(!(y instanceof VarEntry))
			return false;
		
		VarEntry z = (VarEntry) y;
		
		if(z.vid != this.vid)
			return false;
		
		if(z.writer != this.writer || z.size != this.size || z.offset != this.offset)
			return false;
		
		
		if(z.value == null){
			if(this.value != null)
				return false;
		}else{
			if(this.value == null)
				return false;
			else{
				if(!this.value.equalsIgnoreCase(z.value))
					return false;
			}
		}
		
		if(!z.readers.equals(this.readers))
			return false;
		
		return true;
	}
}
