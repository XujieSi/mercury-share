package edu.gatech.traceprocessor.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObjData extends Data {
	public Map<Integer,FldData> fields;

	public ObjData(long addr, int size, int index) {
		super(addr, size, index);
		fields = new HashMap<Integer,FldData>();
	}
	
	@Override
	public void addReader(Read r) {
		super.addReader(r);
		int offset;
		if(r.type == Read.OBJ_ARRAY_ALL){
			offset = FldData.ALL_FIELDS;
			for(Map.Entry<Integer, FldData> fEntry : fields.entrySet())
				if(fEntry.getKey() != FldData.ALL_FIELDS)
					fEntry.getValue().addReader(r);
		}
		else
			offset = r.offset;
		FldData f = fields.get(offset);
		if(f == null){
			f = new FldData(super.addr, r.fldSize, r.offset, this);
			fields.put(offset, f);
			if(offset != FldData.ALL_FIELDS){
				FldData wildCard = fields.get(FldData.ALL_FIELDS);
				if(wildCard != null){
					Write lastWrite = wildCard.getLastWrite();
					if(lastWrite!=null){
						f.addWriter(lastWrite);
					}
				}
			}
		}
		f.addReader(r);
	}

	@Override
	public void addWriter(Write w) {
		super.addWriter(w);
		int offset;
		if(w.type == Read.OBJ_ARRAY_ALL){
			offset = FldData.ALL_FIELDS;
			System.out.println("Warning: a write that may access the whole object inserted. It might introduce unsoundness if no read"
					+ " that may access the whole object before it.");
			for(Map.Entry<Integer, FldData> dEntry : fields.entrySet()){
				if(dEntry.getKey() != FldData.ALL_FIELDS)
					dEntry.getValue().addWriter(w);
			}
		}
		else
			offset = w.offset;
		FldData f = fields.get(offset);
		if(f == null){
			f = new FldData(super.addr, w.fldSize, w.offset, this);
			fields.put(offset, f);
		}
		f.addWriter(w);
	}

	

	@Override
	public List<List<Instruction>> groupAccessors() {
		List<List<Instruction>> ret = new ArrayList<List<Instruction>>();
		Write lw = null;
		for(Instruction i : accessors){
			if(i instanceof Write){
				if(lw != null){
					List<Instruction> lastSeg = ret.get(ret.size() - 1);
					lastSeg.add(i);
				}
				lw  = (Write)i;
				List<Instruction> curSeg = new ArrayList<Instruction>();
				curSeg.add(lw);
				ret.add(curSeg);
			}else if (i instanceof Read){
				if(lw == null)
					continue;
				List<Instruction> curSet = ret.get(ret.size() - 1);
				curSet.add(i);
			}else
				throw new RuntimeException("Unknown instruction " + i);
		}
		return ret;
	}

	@Override
	public String toString() {
		return "ObjData [addr=" + addr + ", size=" + size + "]";
	}

	public Map<Integer, FldData> getFields() {
		return fields;
	}
	
}
