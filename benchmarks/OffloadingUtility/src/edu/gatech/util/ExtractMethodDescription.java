package edu.gatech.util;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.List;

public class ExtractMethodDescription {
	static String genForClass(Class<?> clazz) {
		if (clazz.isPrimitive()) {
			if (clazz.equals(byte.class))
				return "B";
			else if (clazz.equals(char.class))
				return "C";
			else if (clazz.equals(double.class))
				return "D";
			else if (clazz.equals(float.class))
				return "F";
			else if (clazz.equals(int.class))
				return "I";
			else if (clazz.equals(long.class))
				return "J";
			else if (clazz.equals(short.class))
				return "S";
			else if (clazz.equals(boolean.class))
				return "Z";
			else if (clazz.equals(void.class))
				return "V";
			else {
				System.out.println("Unknown primitive class : " + clazz);
			}
		} else {
			return "L";
			// return "L" + clazz.getName() + ";";
		}

		return "X";
	}

	static String genForMethod(Class<?> clazz, Method m) {
		//String res = "L" + m.getClass().getName() + ";.";
		String res = "L" + clazz.getName() + ";.";
		res += m.getName();
		res += "(";

		res += genForClass(m.getReturnType());

		for (Class<?> tmp_clazz : m.getParameterTypes()) {
			res += genForClass(tmp_clazz);
		}
		res += ")";
		return res;
	}

	public static void genForAllMethodsInClass(PrintWriter pr, List<Class<?>> cls){
		for(Class<?> clazz : cls)
			for(Method m : clazz.getDeclaredMethods()) //clazz.getMethods()
				pr.println( genForMethod(clazz, m) );
	}

}
