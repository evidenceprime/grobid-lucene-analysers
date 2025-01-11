package org.grobid.analyzers;

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

public final class MyKoreanPositionFilter extends TokenFilter {

	private final OffsetAttribute offsetAttr = addAttribute(OffsetAttribute.class);

	private int previousOffset=-1;
	
	/** Construct filtering <i>in</i>. */
	public MyKoreanPositionFilter(TokenStream in) {
	    super(in);
	}
	  
	public boolean incrementToken() throws IOException {

		if (!input.incrementToken()) // no more tokens
		    return false; // let's stop there
		  
		while (previousOffset == offsetAttr.startOffset()) {
			if (!input.incrementToken()) return false;
		}
		previousOffset = offsetAttr.startOffset();
		return true;
	}
	  
}
