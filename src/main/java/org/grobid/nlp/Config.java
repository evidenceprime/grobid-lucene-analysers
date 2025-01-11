package org.grobid.nlp;

import java.io.File;

// not used !

public class Config {
    public static String home="";
	static  {
	if (System.getProperty("NLP.home") == null) {
//		for (Entry<Object, Object> e : System.getProperties().entrySet()) {
//			System.err.println(""+e.getKey()+"\t"+e.getValue());
//		}	
	}
	home = System.getProperty("NLP.home");
	
  }
}
