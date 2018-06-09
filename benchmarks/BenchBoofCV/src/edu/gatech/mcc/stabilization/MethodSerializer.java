package edu.gatech.mcc.stabilization;

import java.lang.reflect.Method;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import edu.gatech.util.Utility;

public class MethodSerializer extends Serializer<Method> {

	@Override
	public void write(Kryo kryo, Output output, Method object) {
		byte[] bytes = Utility.objectToBytes(object.getDeclaringClass());
		output.writeInt(bytes.length);
		output.writeBytes(bytes);
		output.writeString(object.toGenericString());	
	}

	@Override
	public Method read(Kryo kryo, Input input, Class<Method> type) {
		int len = input.readInt();
		Class c = (Class)Utility.bytesToObject(input.readBytes(len));
		String consDec = input.readString();
		for(Method con : c.getDeclaredMethods())
			if(con.toGenericString().equals(consDec))
				return con;
		return null;
		}

}
