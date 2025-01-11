package org.grobid.analyzers;

import java.io.IOException;
import java.util.Hashtable;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

public final class MyJapanesePOSFilter extends TokenFilter  {

	  /* Instance variables */
	  Hashtable<String,String> table;
	  private TypeAttribute typeAttr;
	  
	  /**
	   * Construct a filter which removes unspecified pos from the input
	   * TokenStream.
	   */
	  public MyJapanesePOSFilter(TokenStream in, String[] pos) {
		  super(in);
//	    input = in;
	    table = makePOSTable(pos);
	    typeAttr = (TypeAttribute) addAttribute(TypeAttribute.class);
	    
	  }

	  /**
	   * Construct a filter which removes unspecified pos from the input
	   * TokenStream.
	   */
	  public MyJapanesePOSFilter(TokenStream in, Hashtable<String,String> posTable) {
		  super(in);
		  typeAttr = (TypeAttribute) addAttribute(TypeAttribute.class);
		  
	    table = posTable;
	  }

	  /**
	   * Builds a hashtable from an array of pos.
	   */
	  public final static Hashtable<String, String> makePOSTable(String[] pos) {
	    Hashtable<String, String> posTable = new Hashtable<String, String>(pos.length);
	    for (int i = 0; i < pos.length; i++)
	      posTable.put(pos[i], pos[i]);
	    return posTable;
	  }
	  
	  public boolean incrementToken() throws IOException {
		  		  if (!input.incrementToken()) //#B
		  return false; //#C
	
		  if (table.contains(typeAttr.type())) {
			  return true;
		  }
		  return true;
	  }
	  
	}


