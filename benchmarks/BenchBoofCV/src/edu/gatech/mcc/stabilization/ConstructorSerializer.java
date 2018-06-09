package edu.gatech.mcc.stabilization;

import java.lang.reflect.Constructor;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import edu.gatech.util.Utility;

public class ConstructorSerializer extends Serializer<Constructor> {

	@Override
	public void write(Kryo kryo, Output output, Constructor object) {
		byte[] bytes = Utility.objectToBytes(object.getDeclaringClass());
		output.writeInt(bytes.length);
		output.writeBytes(bytes);
		output.writeString(object.toGenericString());
	}

	@Override
	public Constructor read(Kryo kryo, Input input, Class<Constructor> type) {
		int len = input.readInt();
		Class c = (Class)Utility.bytesToObject(input.readBytes(len));
		String consDec = input.readString();
		for(Constructor con : c.getDeclaredConstructors())
			if(con.toGenericString().equals(consDec))
				return con;
		return null;
	}

}
