package org.grobid.analyzers;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

/** Normalizes tokens extracted with {@link MyTokenizer}. */

public final class FilterSeparateHyphen extends TokenFilter {

private Token bufferedToken=null;
private CharTermAttribute termAttr;
private TypeAttribute typeAttr;
private PositionIncrementAttribute posAttr;
private OffsetAttribute offsetAttr;
private int previousOffset=-1;

  /** Construct filtering <i>in</i>. */
  public FilterSeparateHyphen(TokenStream in) {
    super(in);
    termAttr = (CharTermAttribute) addAttribute(CharTermAttribute.class);
    typeAttr=(TypeAttribute) addAttribute(TypeAttribute.class);
    posAttr = (PositionIncrementAttribute) addAttribute(PositionIncrementAttribute.class) ;
    offsetAttr = (OffsetAttribute) addAttribute(OffsetAttribute.class) ;

    in.cloneAttributes();
  }

  public boolean incrementToken() throws IOException {
	  
	  if (bufferedToken != null) { // we have a token waiting to be pop (e.g. "-" then "receptor" in "alpha-receptor") 
		  termAttr.setEmpty(); termAttr.append(bufferedToken.toString());
		  typeAttr.setType(bufferedToken.type()); 
		  posAttr.setPositionIncrement(1);
       	bufferedToken=null;
       	return true;
	    }
	  
	  if (!input.incrementToken()) // no more tokens
	      return false; // let's stop there
	  
	  while (previousOffset == offsetAttr.startOffset()) {
		  if (!input.incrementToken()) return false;
	  }
	  previousOffset = offsetAttr.startOffset();
	  
	  char[] buffer = termAttr.buffer(); 
	  final int bufferLength = termAttr.length();
	  
	  if (bufferLength >= 2 &&
			  isLatinChar(buffer[0])) {
		  int hyphenidx=-1; int i=0;
		  while (hyphenidx==-1 && i<bufferLength-1) {
			  if (buffer[i]=='-') {hyphenidx=i;}
			  i++;
		  }
		  
		  if (hyphenidx > 0) {
		  // create a new token when we face "a-b" will then create "a" and "b"
		  bufferedToken = new Token();    
		  bufferedToken.reinit(termAttr.toString().substring(hyphenidx+1), offsetAttr.startOffset()+hyphenidx+1, offsetAttr.endOffset());
		  termAttr.setLength(hyphenidx);offsetAttr.setOffset(offsetAttr.startOffset()+hyphenidx,offsetAttr.endOffset());
		 
	      }
	  } else if (buffer[0]==')' && bufferLength>1) {
		  bufferedToken = new Token();    
		  bufferedToken.reinit(termAttr.toString().substring(1), offsetAttr.startOffset()+1, offsetAttr.endOffset());
		  termAttr.setLength(1);offsetAttr.setOffset(offsetAttr.startOffset(),offsetAttr.startOffset()+1);
	  }
	  
	  return true;
  }
  private boolean isLatinChar(char c) {
		return ((c >='a' && c<='z') || (c >='A' && c<='Z') || (c >='0' && c<='9'));

	}
  
}

