package edu.gatech.traceprocessor.parser;

public class Write extends Instruction{
	public int type;
	public long addr;
	public int offset;
	public int objSize;
	public int fldSize;
	public Data data;
	
	public Write(int lineNum, int threadID, Method method, int type, long addr, int offset, int objSize, int fldSize) {
		super(lineNum, threadID, method);
		this.type = type;
		this.addr = addr;
		this.offset = offset;
		this.objSize = objSize;
		this.fldSize = fldSize;
		Program p = this.getProgram();
		DataKey dataKey = p.getDataKey(addr);
		if( type == Read.OBJ_PARAM || type == Read.OBJ_RET){
			throw new RuntimeException("Parameter passing and return value passing are not inserted directly!");
		}
		if(dataKey!=null)
			data = p.addrToData.get(dataKey);
		if(dataKey == null || data == null){
			int index = p.newAddr(addr);
			dataKey = p.getDataKey(addr);
			if(type == Read.OBJ_DUMP || type == Read.OBJ_STATIC || type == Read.OBJ_JNI_STATIC || type == Read.OBJ_RET){
				data = new RegOrStaticData(addr,objSize,dataKey.index);
			}
			else if (type == Read.OBJ_REF || type == Read.OBJ_JNI || type == Read.OBJ_ARRAY || type == Read.OBJ_JNI_ARRAY || type == Read.OBJ_ARRAY_ALL){
				System.out.println("Wow, this address has never been used! Read without allocation for address "+addr+" in "+method);
				data = new ObjData(addr,objSize,dataKey.index);
			}else
					throw new RuntimeException("Read type "+type+" should not be actually added!");
			p.addrToData.put(dataKey, data);
		}
		data.addWriter(this);
	}

	@Override
	public String toPlainFormat() {
		return this.getThreadID()+"<w t="+(int)type+" v="+addr+" p="+offset+" s="+objSize+"/>";
	}
	
	public boolean isArrayAccess(){
		return type == Read.OBJ_ARRAY || type == Read.OBJ_JNI_ARRAY || type == Read.OBJ_ARRAY_ALL;
	}
	
	public boolean mayAccessWholeArray(){
		return type == Read.OBJ_ARRAY_ALL;
	}
	
	public boolean isHoldArray(){
		return type == Read.OBJ_JNI_HOLD_ARRAY;
	}
	
	public boolean isRelArray(){
		return type == Read.OBJ_JNI_REL_ARRAY;
	}

}
