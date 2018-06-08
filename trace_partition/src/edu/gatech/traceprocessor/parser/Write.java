package edu.gatech.traceprocessor.parser;

public class Write extends Instruction{
	public int type;
	public long addr;
	public long from;
	public int from_type;
	public int offset;
	public int objSize;
	public int fldSize;
	public Data data;
	public String typeName;
	
	public Write(int lineNum, int threadID, Method method, int type, long addr, int offset, int objSize, int fldSize, String _typeName, int _from_type, long _from) {
		super(lineNum, threadID, method);
		this.type = type;
		this.from_type = _from_type;
		this.from = _from;
		this.addr = addr;
		if(type == Configuration.OBJ_ARRAY)
			offset = offset/Configuration.defFldSize;
		this.offset = offset;
		this.objSize = objSize;
		this.fldSize = fldSize;
		this.typeName = _typeName;
		
		Program p = this.getProgram();
		DataKey dataKey = p.getDataKey(addr);
		if( type == Configuration.OBJ_PARAM || type == Configuration.OBJ_RET){
			throw new RuntimeException("Parameter passing and return value passing are not inserted directly!");
		}
		if(dataKey!=null)
			data = p.addrToData.get(dataKey);
		if(dataKey == null || data == null){
			int index = p.newAddr(addr);
			dataKey = p.getDataKey(addr);
			if(type == Configuration.OBJ_DUMP || type == Configuration.OBJ_STATIC || type == Configuration.OBJ_JNI_STATIC || type == Configuration.OBJ_RET || type == Configuration.OBJ_NATIVE){
				data = new RegOrStaticData(addr,objSize,dataKey.index, _typeName);
			}
			else if (type == Configuration.OBJ_REF || type == Configuration.OBJ_JNI || type == Configuration.OBJ_ARRAY || type == Configuration.OBJ_JNI_ARRAY || type == Configuration.OBJ_ARRAY_ALL){
				System.out.println("Write without ALLOC for address "+addr+" in "+method);
				data = new ObjData(addr,objSize,dataKey.index, _typeName);
			}else
					throw new RuntimeException("Type "+type+" is UNKOWN");
			p.addrToData.put(dataKey, data);
		}
		
		if(data.typeName.startsWith("UNKOWN")) {
			//data.typeName = Data.extractClassType(_typeName);
			throw new RuntimeException("Write: data.typeName is UNKOWN");
		}
		else if(_typeName.contains(data.typeName) == false){
			System.out.println("W:Found type inconsistency for addr=" + addr +", existing type:" + data.typeName + ", expected type=" + _typeName);
		}
		data.addWriter(this);
	}

	@Override
	public String toPlainFormat() {
		return this.getThreadID()+"<w t="+(int)type+" v="+addr+" p="+offset+" s="+objSize+"/>";
	}
	
	public boolean isArrayAccess(){
		return type == Configuration.OBJ_ARRAY || type == Configuration.OBJ_JNI_ARRAY || type == Configuration.OBJ_ARRAY_ALL;
	}
	
	public boolean mayAccessWholeArray(){
		return type == Configuration.OBJ_ARRAY_ALL;
	}
	
	public boolean isHoldArray(){
		return type == Configuration.OBJ_JNI_HOLD_ARRAY;
	}
	
	public boolean isRelArray(){
		return type == Configuration.OBJ_JNI_REL_ARRAY;
	}

}
