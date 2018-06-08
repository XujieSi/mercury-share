package edu.gatech.traceprocessor.parser;

public class Read extends Instruction {
	/*
	 * The read/write type constants
	 */
	public static final int OBJ_PARAM = 0;
	public static final int OBJ_STATIC = 1;
	public static final int OBJ_REF = 2;
	public static final int OBJ_JNI = 3;
	public static final int OBJ_JNI_STATIC = 4;
	public static final int OBJ_JNI_HOLD_ARRAY = 5;
	public static final int OBJ_JNI_REL_ARRAY = 6;
	public static final int OBJ_ARRAY = 7;
	public static final int OBJ_JNI_ARRAY = 8;
	public static final int OBJ_ARRAY_ALL = 9;
	public static final int OBJ_RET = 10;
	public static final int OBJ_DUMP = 11; // used to insert fake read/write for parameter passing and return values
	public int type;
	public long addr;
	public int offset;
	public int objSize;
	public int fldSize;
	public static long fakeAddr = 0;
	public Data data;

	public Read(int lineNum, int threadID, Method method, int type, long addr, int offset, int objSize, int fldSize) {
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
		if(dataKey != null)
			data = p.addrToData.get(dataKey);
		if(dataKey == null ||data == null){
			if(dataKey!=null&&dataKey.index !=0 )
				throw new RuntimeException("Something wrong in the program logic for allocation");
			int index = p.newAddr(addr);
			dataKey = p.getDataKey(addr);
			if(type == OBJ_DUMP || type == OBJ_RET || type == OBJ_STATIC || type == OBJ_JNI_STATIC){
				System.out.println("Read without write for address "+addr+" in "+method);
				data = new RegOrStaticData(addr,objSize,dataKey.index);
			}
			else if (type == OBJ_REF || type == OBJ_JNI || type == OBJ_ARRAY || type == OBJ_JNI_ARRAY || type == OBJ_ARRAY_ALL){
				System.out.println("Wow, this address has never been used! Read without allocation for address "+addr+" in "+method);
				data = new ObjData(addr,objSize,dataKey.index);
			}
			else
				throw new RuntimeException("Read type "+type+" should not be actually added!");
			p.addrToData.put(dataKey, data);
		}
		data.addReader(this);
	}

	@Override
	public String toPlainFormat() {
		return this.getThreadID()+"<r t="+(int)type+" v="+addr+" p="+offset+" s="+objSize+"/>";
	}
	
	public boolean isArrayAccess(){
		return type == OBJ_ARRAY || type == OBJ_JNI_ARRAY || type == OBJ_ARRAY_ALL || type == OBJ_JNI_HOLD_ARRAY || type == OBJ_JNI_REL_ARRAY;
	}
	
	public boolean mayAccessWholeArray(){
		return type == OBJ_ARRAY_ALL;
	}
	
	public boolean isHoldArray(){
		return type == OBJ_JNI_HOLD_ARRAY;
	}
	
	public boolean isRelArray(){
		return type == OBJ_JNI_REL_ARRAY;
	}
	
	public static long genFakeAddr(){
		fakeAddr --;
		return fakeAddr;
	}

}
