package edu.gatech.traceprocessor.parser;

public class Read extends Instruction {
	public int type;
	public long addr;
	public int offset;
	public int objSize;
	public int fldSize;
	public static long fakeAddr = 0;
	public Data data;
	public String typeName;

	public Read(int lineNum, int threadID, Method method, int type, long addr,
			int offset, int objSize, int fldSize, String _typeName) {
		super(lineNum, threadID, method);
		this.type = type;
		this.addr = addr;
		if (type == Configuration.OBJ_ARRAY)
			offset = offset / Configuration.defFldSize;
		this.offset = offset;
		this.objSize = objSize;
		this.fldSize = fldSize;
		this.typeName = _typeName;

		Program p = this.getProgram();
		DataKey dataKey = p.getDataKey(addr);

		if (type == Configuration.OBJ_PARAM || type == Configuration.OBJ_RET) {
			throw new RuntimeException(
					"Parameter passing and return value passing are not inserted directly!");
		}

		if (dataKey != null)
			data = p.addrToData.get(dataKey);
		if (dataKey == null || data == null) {
			// if(dataKey!=null&&dataKey.index !=0 )
			// throw new
			// RuntimeException("Something wrong in the program logic for allocation");

			int index = p.newAddr(addr);
			dataKey = p.getDataKey(addr);
			if (type == Configuration.OBJ_DUMP || type == Configuration.OBJ_RET
					|| type == Configuration.OBJ_STATIC
					|| type == Configuration.OBJ_JNI_STATIC) {
				System.out.println("Read without WRITE for address " + addr
						+ " in " + method);
				data = new RegOrStaticData(addr, objSize, dataKey.index,
						_typeName);
			} else if (type == Configuration.OBJ_REF
					|| type == Configuration.OBJ_JNI
					|| type == Configuration.OBJ_ARRAY
					|| type == Configuration.OBJ_JNI_ARRAY
					|| type == Configuration.OBJ_ARRAY_ALL) {
				System.out.println("Read without ALLOCATION for address "
						+ addr + " in " + method);
				data = new ObjData(addr, objSize, dataKey.index, _typeName);
			} else if (type == Configuration.OBJ_NATIVE) {// memory access in native code, an array of bytes
				System.out.println("Read without WRITE for NATIVE address "
						+ addr + " in " + method);
				data = new RegOrStaticData(addr, objSize, dataKey.index,
						_typeName);
			} else
				throw new RuntimeException("Type " + type + " is UNKOWN");
			p.addrToData.put(dataKey, data);
		}

		if (data.typeName.startsWith("UNKOWN")){
			throw new RuntimeException("Read: data.typeName is UNKOWN");
			//data.typeName = Data.extractClassType(_typeName);
		}
		else if (_typeName.contains(data.typeName) == false) {
			System.out.println("R:Found type inconsistency for addr=" + addr
					+ ", existing type:" + data.typeName + ", expected type="
					+ _typeName);

		}

		data.addReader(this);
	}

	@Override
	public String toPlainFormat() {
		return this.getThreadID() + "<r t=" + (int) type + " v=" + addr + " p="
				+ offset + " s=" + objSize + "/>";
	}

	
	public boolean isArrayAccess() {
		return type == Configuration.OBJ_ARRAY || type == Configuration.OBJ_JNI_ARRAY
				|| type == Configuration.OBJ_ARRAY_ALL || type == Configuration.OBJ_JNI_HOLD_ARRAY
				|| type == Configuration.OBJ_JNI_REL_ARRAY;
	}

	public boolean mayAccessWholeArray() {
		return type == Configuration.OBJ_ARRAY_ALL;
	}

	public boolean isHoldArray() {
		return type == Configuration.OBJ_JNI_HOLD_ARRAY;
	}

	public boolean isRelArray() {
		return type == Configuration.OBJ_JNI_REL_ARRAY;
	}

	public static long genFakeAddr() {
		fakeAddr--;
		return fakeAddr;
	}

}
