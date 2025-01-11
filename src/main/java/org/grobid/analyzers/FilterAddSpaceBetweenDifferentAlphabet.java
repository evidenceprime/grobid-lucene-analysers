package org.grobid.analyzers;

import java.io.IOException;
import java.lang.Character.UnicodeBlock;
import java.util.LinkedList;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

public final class FilterAddSpaceBetweenDifferentAlphabet extends TokenFilter {
	private final LinkedList<String[]> tokenQueue = new LinkedList<String[]>();
	
  private final CharTermAttribute termAttr = addAttribute(CharTermAttribute.class);;
  private final TypeAttribute typeAttr = addAttribute(TypeAttribute.class);
  private final PositionIncrementAttribute posAttr = addAttribute(PositionIncrementAttribute.class);
  private final OffsetAttribute offsetAttr = addAttribute(OffsetAttribute.class);
	
	public FilterAddSpaceBetweenDifferentAlphabet (TokenStream input) {
		super(input);
	}
	
	public boolean incrementToken() throws IOException {
		
		if(tokenQueue.size()>0) {
			String[] popToken=tokenQueue.removeFirst();
			if (popToken != null && popToken[0] != null) {
				termAttr.setEmpty(); termAttr.append(popToken[0]);
				posAttr.setPositionIncrement(1);
				typeAttr.setType(popToken[1]);
				offsetAttr.setOffset(Integer.parseInt(popToken[2]), Integer.parseInt(popToken[2])+popToken[0].length());
				return true;
			}
		}
		if (!input.incrementToken()) //#B
			  return false; //#C
		char[] buffer = termAttr.buffer(); 
	    final int bufferLength = termAttr.length();
	    String currentType=typeAttr.type();
	    int currentStartOffset=offsetAttr.startOffset();
	    

	    boolean charModified=false;
		  // Here we convert any Japanese punctuation to its usual Latin counterpart
		  // see for example: http://en.wikipedia.org/wiki/Japanese_typographic_symbols	    
	    UnicodeBlock prec=null;  String currentString="";
	    for (int i=0; i<bufferLength; i++) {
	    	char c= buffer[i]; 
	    	if (prec!=null) {
	    		
	    		if (! prec.equals(Character.UnicodeBlock.of(c))) {
	    			charModified=true;
	    			String[] tok = new String[4];; // =new String[]();
	    			tok[0]=currentString;tok[1]=currentType; 
	    			tok[2]=String.valueOf(currentStartOffset);
	    			tokenQueue.add(tok);
	    			currentString="";
	    			currentStartOffset=currentStartOffset+i;
	    			prec=Character.UnicodeBlock.of(c);
	    		}
	    	} else {
	    		prec=Character.UnicodeBlock.of(c);
	    	}
	    	currentString+=c;
	    }
	    if (charModified) {
	    	// add the rest of the string to the stack
	    	String[] tok = new String[4];
			tok[0]=currentString;tok[1]=currentType; 
			tok[2]=String.valueOf(currentStartOffset);
			tokenQueue.add(tok);
			// and pop the first element of the stack
	    	String[] popToken=tokenQueue.removeFirst();
			termAttr.setEmpty(); termAttr.append(popToken[0]);
			posAttr.setPositionIncrement(1);
			typeAttr.setType(popToken[1]);
			offsetAttr.setOffset(Integer.parseInt(popToken[2]), Integer.parseInt(popToken[2])+popToken[0].length());
			return true;
	    } 
	    // no modification, let's go
	    return true;
	}

  @Override
  public void reset() throws IOException {
    super.reset();
    tokenQueue.clear();
  }
}
