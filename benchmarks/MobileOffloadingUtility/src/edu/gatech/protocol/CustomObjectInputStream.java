package edu.gatech.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

public class CustomObjectInputStream extends ObjectInputStream {
	ClassLoader customLoader;  
    
    public CustomObjectInputStream ( InputStream in, ClassLoader loader ) throws IOException, SecurityException {  
        super( in );  
        customLoader= loader;  
    }  
    @Override
    protected Class<?> resolveClass( ObjectStreamClass v ) throws IOException, ClassNotFoundException {  
        if ( customLoader == null )  
            return super.resolveClass( v );  
        else{  
        	try{
            return Class.forName( v.getName(), true, customLoader);  
        	}
        	catch(ClassNotFoundException e){
        		return super.resolveClass( v );  
        	}
        }
    }  

}
