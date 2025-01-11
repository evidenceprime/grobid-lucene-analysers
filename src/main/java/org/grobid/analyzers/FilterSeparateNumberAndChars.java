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

/** Splits tokens containing digits attached to letters (e.g. 80Mh=>80 Mh) */

public final class FilterSeparateNumberAndChars extends TokenFilter {

private Token bufferedToken=null;
private Token bufferedTokenPlus=null;
private CharTermAttribute termAttr;
private TypeAttribute typeAttr;
private PositionIncrementAttribute posAttr;
private OffsetAttribute offsetAttr;

  /** Construct filtering <i>in</i>. */
  public FilterSeparateNumberAndChars(TokenStream in) {
    super(in);
    termAttr = (CharTermAttribute) addAttribute(CharTermAttribute.class);
    typeAttr=(TypeAttribute) addAttribute(TypeAttribute.class);
    posAttr = (PositionIncrementAttribute) addAttribute(PositionIncrementAttribute.class) ;
    offsetAttr = (OffsetAttribute) addAttribute(OffsetAttribute.class) ;

    in.cloneAttributes();
  }

  public boolean incrementToken() throws IOException {
	  
	  if (bufferedToken != null) { // we have a token waiting to be pop (e.g. "80" then "hz" in "80hz") 
		  termAttr.setEmpty(); termAttr.append(bufferedToken.toString());
		  typeAttr.setType(bufferedToken.type()); 
		  posAttr.setPositionIncrement(1);
		  if (bufferedTokenPlus!=null) {
			  bufferedToken=bufferedTokenPlus;
			  bufferedTokenPlus=null;
		  } else {
			  bufferedToken=null;
		  }
       	return true;
	    }
	  if (!input.incrementToken()) // no more tokens
	      return false; // let's stop there
	  
	  char[] buffer = termAttr.buffer(); 
	  final int bufferLength = termAttr.length();
	  
	  if (bufferLength >= 2 &&
			  isDigit(buffer[0])) {
		  char lastChar=buffer[bufferLength-1];
		  if (! isDigit(lastChar) 
				  && lastChar!='-'
				  && lastChar!='.'
				  && lastChar!='('
				  && lastChar!=')'
					  && lastChar!='\''
		  ) {

			  int idx=-1; int i=0;
			  while (idx==-1 && i<bufferLength-1) {
				  if (!isDigit(buffer[i])) {idx=i;}
				  i++;
			  }

			  if (idx > 0) {
				  // create a new token when we face "80hz" will then create "80" and "mz"
				  bufferedToken = new Token();    
				  bufferedToken.reinit(termAttr.toString().substring(idx), offsetAttr.startOffset()+idx+1, offsetAttr.endOffset());
				  termAttr.setLength(idx);offsetAttr.setOffset(offsetAttr.startOffset()+idx,offsetAttr.endOffset());		 
				  bufferedTokenPlus=null;
			  }

		  } else {
			  // detect the case 2010-2012
			  if (isDigit(lastChar)) {
				  int i=1;
				  while (i<bufferLength && isDigit(buffer[i])) i++;
				  if (i<bufferLength && buffer[i] == '-') {
					  boolean ok=true;
					  int idx=i+1;
					  while (idx < bufferLength && ok) {
						  if (! isDigit(buffer[idx])) {ok=false;}
						  idx++;
					  }
					  if (ok) { 
						  bufferedToken = new Token();    
						  bufferedToken.reinit("-" , offsetAttr.startOffset()+i, offsetAttr.startOffset()+i+1);

						  bufferedTokenPlus = new Token();    
						  bufferedTokenPlus.reinit(termAttr.toString().substring(i+1), offsetAttr.startOffset()+i+1, offsetAttr.endOffset());
						  termAttr.setLength(i);offsetAttr.setOffset(offsetAttr.startOffset()+i,offsetAttr.endOffset());		 
					  }
				  }
			  }
		  }
	  }
	  
	  return true;
  }
  private boolean isDigit(char c) {
		return (c >='0' && c<='9');

	}
  
}

