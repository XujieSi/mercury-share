package edu.gatech.mcc.stabilization;

import georegression.struct.point.Point2D_I16;
import boofcv.struct.QueueCorner;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class QueueCornerSerializer extends Serializer<QueueCorner> {

	@Override
	public QueueCorner read(Kryo arg0, Input arg1, Class<QueueCorner> arg2) {
		int size = arg1.readInt();
		QueueCorner ret = new QueueCorner(size);
		for(int i = 0; i < size; i++){
			ret.add(arg1.readInt(),arg1.readInt());
		}
		return ret;
	}

	@Override
	public void write(Kryo arg0, Output arg1, QueueCorner arg2) {
		int size = arg2.getSize();
		arg1.writeInt(size);
		for(int i = 0; i < size; i++){
			Point2D_I16 temp = arg2.get(i);
			arg1.writeInt(temp.x);
			arg1.writeInt(temp.y);
		}
	}

}
